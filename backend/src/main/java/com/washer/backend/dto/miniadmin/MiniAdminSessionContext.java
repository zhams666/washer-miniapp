package com.washer.backend.dto.miniadmin;

import com.washer.backend.entity.MiniAdminStaff;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MiniAdminSessionContext {

    private MiniAdminStaff staff;
    private boolean platformScope;
    private List<MiniAdminStoreOption> stores;
    private List<String> permissions;
}
