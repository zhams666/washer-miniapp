# Points Mall Balance Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Display the current logged-in user's persisted points in the mini-program points mall.

**Architecture:** Reuse the existing `GET /wallet/summary?userId=...` client API, which is already the source for the points statistic on the Mine page. The points-mall page refreshes this value on every `onShow`, so navigation and user switching cannot retain stale data.

**Tech Stack:** WeChat Mini Program, TypeScript, existing `utils/user.ts` session helpers, existing `apis/wallet.ts` API client.

## Global Constraints

- Preserve the existing `/wallet/summary` response contract and backend implementation.
- Do not display a prior user's points when there is no active session.
- Keep the change scoped to the points-mall page and its verification documentation.

---

### Task 1: Load Current User Points In Points Mall

**Files:**
- Modify: `pages/points-mall/index.ts`
- Test: TypeScript project compilation via `npx tsc --noEmit`

**Interfaces:**
- Consumes: `isLoggedIn(): boolean` and `getCachedUserId(): number | null` from `utils/user.ts`.
- Consumes: `getWalletSummary(userId: number): Promise<IObject>` from `apis/wallet.ts`.
- Produces: `loadPoints(): Promise<void>` which updates the page's `points: number` data field.

- [ ] **Step 1: Add a refresh test scenario**

In the mini-program developer tools, log in as test user 2 whose `/wallet/summary` returns `{ "points": 9 }`, open the Mine page, and tap Points Mall. Confirm the mall must display `9`, not its initial `0`.

- [ ] **Step 2: Verify the current page cannot meet the scenario**

Inspect `pages/points-mall/index.ts`. It only declares `points: 0` and has no lifecycle method or API call, so the displayed points always remain `0`.

- [ ] **Step 3: Implement the page refresh**

```ts
onShow() {
  this.loadPoints();
},

async loadPoints() {
  const userId = getCachedUserId();
  if (!isLoggedIn() || !userId) {
    this.setData({ points: 0 });
    return;
  }

  const summary = await getWalletSummary(userId);
  const points = Number(summary && summary.points !== undefined ? summary.points : 0);
  this.setData({ points: Number.isFinite(points) ? points : 0 });
}
```

- [ ] **Step 4: Compile the mini-program TypeScript sources**

Run: `npx tsc --noEmit`

Expected: successful compilation, with no new type errors from `pages/points-mall/index.ts`.

- [ ] **Step 5: Commit**

```bash
git add pages/points-mall/index.ts docs/superpowers/plans/2026-08-27-points-mall-balance-sync.md
git commit -m "fix: load current user points in mall"
```
