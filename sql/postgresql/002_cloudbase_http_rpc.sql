-- Run this file once in CloudBase PostgreSQL SQL editor after 001_cloudbase_init.sql.

CREATE OR REPLACE FUNCTION redeem_points_product(
  p_user_id BIGINT,
  p_product_id BIGINT,
  p_request_no VARCHAR DEFAULT NULL
)
RETURNS SETOF point_redemption_order
LANGUAGE plpgsql
AS $$
DECLARE
  v_product point_mall_product%ROWTYPE;
  v_order point_redemption_order%ROWTYPE;
  v_redemption_no VARCHAR(64);
  v_points INTEGER;
BEGIN
  IF p_user_id IS NULL OR p_user_id <= 0 OR p_product_id IS NULL OR p_product_id <= 0 THEN
    RAISE EXCEPTION 'userId and productId are required';
  END IF;

  IF p_request_no IS NOT NULL AND btrim(p_request_no) <> '' THEN
    SELECT * INTO v_order
    FROM point_redemption_order
    WHERE user_id = p_user_id AND request_no = btrim(p_request_no);
    IF FOUND THEN
      RETURN NEXT v_order;
      RETURN;
    END IF;
  ELSE
    p_request_no := NULL;
  END IF;

  PERFORM 1 FROM user_info WHERE id = p_user_id FOR UPDATE;
  IF NOT FOUND THEN
    RAISE EXCEPTION '用户不存在';
  END IF;

  SELECT * INTO v_product FROM point_mall_product WHERE id = p_product_id FOR UPDATE;
  IF NOT FOUND OR v_product.status <> 1 THEN
    RAISE EXCEPTION '积分商品不可兑换';
  END IF;
  IF (v_product.effective_time IS NOT NULL AND v_product.effective_time > CURRENT_TIMESTAMP)
     OR (v_product.expire_time IS NOT NULL AND v_product.expire_time <= CURRENT_TIMESTAMP) THEN
    RAISE EXCEPTION '积分商品不在兑换时间内';
  END IF;
  IF v_product.points_price IS NULL OR v_product.points_price <= 0 THEN
    RAISE EXCEPTION '积分商品价格不合法';
  END IF;
  IF v_product.stock_total <= 0 THEN
    RAISE EXCEPTION '商品库存不足';
  END IF;
  IF v_product.limit_per_user > 0 AND EXISTS (
    SELECT 1 FROM point_redemption_order
    WHERE user_id = p_user_id AND product_id = p_product_id AND fulfillment_status <> 'cancelled'
  ) THEN
    IF (SELECT COUNT(*) FROM point_redemption_order
        WHERE user_id = p_user_id AND product_id = p_product_id AND fulfillment_status <> 'cancelled') >= v_product.limit_per_user THEN
      RAISE EXCEPTION '已达到该商品的限兑次数';
    END IF;
  END IF;

  v_points := v_product.points_price;
  UPDATE user_info SET points = points - v_points
  WHERE id = p_user_id AND points >= v_points;
  IF NOT FOUND THEN
    RAISE EXCEPTION '积分不足';
  END IF;
  UPDATE point_mall_product SET stock_total = stock_total - 1 WHERE id = p_product_id;

  v_redemption_no := 'PR' || substr(md5(random()::text || clock_timestamp()::text || p_user_id::text), 1, 18);
  INSERT INTO point_redemption_order (
    redemption_no, request_no, user_id, product_id, product_title_snapshot, points_amount, fulfillment_status, remark
  ) VALUES (
    v_redemption_no, p_request_no, p_user_id, p_product_id, v_product.title, v_points, 'pending', 'point mall redemption'
  ) RETURNING * INTO v_order;
  RETURN NEXT v_order;
END;
$$;

CREATE OR REPLACE FUNCTION merge_user_account(
  p_source_user_id BIGINT,
  p_target_user_id BIGINT
)
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
  v_table TEXT;
BEGIN
  IF p_source_user_id IS NULL OR p_target_user_id IS NULL OR p_source_user_id = p_target_user_id THEN
    RETURN;
  END IF;

  UPDATE user_store_wallet AS target
  SET principal_balance = COALESCE(target.principal_balance, 0) + COALESCE(source.principal_balance, 0),
      available_principal_balance = COALESCE(target.available_principal_balance, 0) + COALESCE(source.available_principal_balance, 0),
      frozen_principal_balance = COALESCE(target.frozen_principal_balance, 0) + COALESCE(source.frozen_principal_balance, 0),
      gift_balance = COALESCE(target.gift_balance, 0) + COALESCE(source.gift_balance, 0),
      available_gift_balance = COALESCE(target.available_gift_balance, 0) + COALESCE(source.available_gift_balance, 0),
      frozen_gift_balance = COALESCE(target.frozen_gift_balance, 0) + COALESCE(source.frozen_gift_balance, 0),
      total_recharge_principal = COALESCE(target.total_recharge_principal, 0) + COALESCE(source.total_recharge_principal, 0),
      total_recharge_gift = COALESCE(target.total_recharge_gift, 0) + COALESCE(source.total_recharge_gift, 0),
      total_consume_principal = COALESCE(target.total_consume_principal, 0) + COALESCE(source.total_consume_principal, 0),
      total_consume_gift = COALESCE(target.total_consume_gift, 0) + COALESCE(source.total_consume_gift, 0),
      total_refund_principal = COALESCE(target.total_refund_principal, 0) + COALESCE(source.total_refund_principal, 0),
      status = CASE WHEN COALESCE(target.status, 1) = 1 OR COALESCE(source.status, 1) = 1 THEN 1 ELSE target.status END,
      version = COALESCE(target.version, 0) + 1, updated_at = CURRENT_TIMESTAMP
  FROM user_store_wallet AS source
  WHERE source.user_id = p_source_user_id AND target.user_id = p_target_user_id AND target.store_id = source.store_id;
  DELETE FROM user_store_wallet AS source USING user_store_wallet AS target
  WHERE target.user_id = p_target_user_id AND target.store_id = source.store_id AND source.user_id = p_source_user_id;
  UPDATE user_store_wallet SET user_id = p_target_user_id, updated_at = CURRENT_TIMESTAMP WHERE user_id = p_source_user_id;

  DELETE FROM user_daily_discount_record AS source USING user_daily_discount_record AS target
  WHERE target.user_id = p_target_user_id AND target.discount_date = source.discount_date
    AND target.discount_type = source.discount_type
    AND COALESCE(target.discount_scope, '') = COALESCE(source.discount_scope, '')
    AND COALESCE(target.scope_store_id, 0) = COALESCE(source.scope_store_id, 0)
    AND source.user_id = p_source_user_id;
  UPDATE user_daily_discount_record SET user_id = p_target_user_id WHERE user_id = p_source_user_id;

  FOREACH v_table IN ARRAY ARRAY[
    'user_membership_log', 'user_vehicle', 'wallet_transaction', 'recharge_order', 'card_purchase_order',
    'user_card', 'card_usage_record', 'wash_order', 'wash_order_payment_detail', 'payment_transaction',
    'store_settlement_detail', 'mini_admin_asset_operation'
  ] LOOP
    EXECUTE format('UPDATE %I SET user_id = $1 WHERE user_id = $2', v_table) USING p_target_user_id, p_source_user_id;
  END LOOP;

  UPDATE user_info
  SET openid = NULL, unionid = NULL, mobile = NULL, user_status = 0,
      remark = '手机号登录合并至用户 ' || p_target_user_id, updated_at = CURRENT_TIMESTAMP
  WHERE id = p_source_user_id;
END;
$$;
