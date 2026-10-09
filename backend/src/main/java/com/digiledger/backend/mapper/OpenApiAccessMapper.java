package com.digiledger.backend.mapper;

import com.digiledger.backend.openapi.OpenApiAccess;
import org.apache.ibatis.annotations.*;

@Mapper
public interface OpenApiAccessMapper {
    @Select("SELECT enabled, allow_write AS allowWrite, token_hash AS tokenHash, token_prefix AS tokenPrefix, updated_at AS updatedAt FROM open_api_access WHERE id = 1")
    OpenApiAccess get();

    @Update("UPDATE open_api_access SET enabled = #{enabled}, allow_write = #{allowWrite} WHERE id = 1")
    void update(@Param("enabled") boolean enabled, @Param("allowWrite") boolean allowWrite);

    @Update("UPDATE open_api_access SET token_hash = #{hash}, token_prefix = #{prefix} WHERE id = 1")
    void rotate(@Param("hash") String hash, @Param("prefix") String prefix);
}
