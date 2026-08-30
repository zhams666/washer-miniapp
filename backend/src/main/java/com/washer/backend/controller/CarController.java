package com.washer.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.washer.backend.common.ApiResponse;
import com.washer.backend.entity.UserVehicle;
import com.washer.backend.mapper.UserVehicleMapper;
import java.util.Collections;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/car")
public class CarController {

    private final UserVehicleMapper userVehicleMapper;

    public CarController(UserVehicleMapper userVehicleMapper) {
        this.userVehicleMapper = userVehicleMapper;
    }

    @GetMapping("/getList")
    public ApiResponse<List<UserVehicle>> getList(
        @RequestParam(required = false) Long id,
        @RequestParam(required = false) Long userId
    ) {
        Long resolvedUserId = userId != null ? userId : id;
        if (resolvedUserId == null) {
            return ApiResponse.success(Collections.emptyList());
        }
        List<UserVehicle> vehicles = userVehicleMapper.selectList(
            new LambdaQueryWrapper<UserVehicle>()
                .eq(UserVehicle::getUserId, resolvedUserId)
                .orderByDesc(UserVehicle::getId)
        );
        return ApiResponse.success(vehicles);
    }
}
