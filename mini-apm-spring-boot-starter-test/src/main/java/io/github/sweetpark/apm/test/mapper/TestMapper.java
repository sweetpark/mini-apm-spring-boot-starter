package io.github.sweetpark.apm.test.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TestMapper {

    @Select("SELECT 1")
    int selectOne();

    @Select("SELECT #{param}")
    String selectParam(@Param("param") String param);

    @Select("SELECT COUNT(*) FROM NON_EXISTENT_TABLE")
    int selectError();
}