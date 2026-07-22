package com.digiledger.backend.mapper;

import com.digiledger.backend.model.entity.DashboardSnapshot;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface DashboardSnapshotMapper {
    void upsert(DashboardSnapshot snapshot);
    List<DashboardSnapshot> findRecent();
}
