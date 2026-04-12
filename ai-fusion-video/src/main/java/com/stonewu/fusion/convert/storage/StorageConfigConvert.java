package com.stonewu.fusion.convert.storage;

import com.stonewu.fusion.controller.storage.vo.StorageConfigRespVO;
import com.stonewu.fusion.entity.storage.StorageConfig;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 存储配置 MapStruct 转换器
 */
@Mapper
public interface StorageConfigConvert {

    StorageConfigConvert INSTANCE = Mappers.getMapper(StorageConfigConvert.class);

    StorageConfigRespVO convert(StorageConfig config);

    List<StorageConfigRespVO> convertList(List<StorageConfig> list);
}
