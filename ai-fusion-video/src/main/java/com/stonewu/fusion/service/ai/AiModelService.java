package com.stonewu.fusion.service.ai;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stonewu.fusion.common.PageResult;
import com.stonewu.fusion.common.BusinessException;
import com.stonewu.fusion.entity.ai.AiModel;
import com.stonewu.fusion.mapper.ai.AiModelMapper;
import com.stonewu.fusion.service.ai.agentscope.AgentScopeModelFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cn.hutool.core.util.StrUtil;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiModelService {

    private final AiModelMapper aiModelMapper;
    private final ModelPresetService modelPresetService;
    private final ChatModelFactory chatModelFactory;
    private final AgentScopeModelFactory agentScopeModelFactory;

    @Transactional
    public Long createAiModel(AiModel aiModel) {
        boolean exists = aiModelMapper.exists(new LambdaQueryWrapper<AiModel>().eq(AiModel::getCode, aiModel.getCode()));
        if (exists) {
            throw new BusinessException(400, "模型标识已存在");
        }
        // 如果用户未设置 config，尝试从预设自动填充
        if (StrUtil.isBlank(aiModel.getConfig()) && StrUtil.isNotBlank(aiModel.getCode())) {
            String presetConfig = modelPresetService.getPresetConfig(aiModel.getCode());
            if (presetConfig != null) {
                aiModel.setConfig(presetConfig);
            }
        }
        aiModelMapper.insert(aiModel);
        return aiModel.getId();
    }

    @Transactional
    public void updateAiModel(Long id, String name, String code, Integer modelType,
                               String icon, String description, Integer sort,
                               Integer status, String config, Boolean defaultModel,
                               Long apiConfigId) {
        AiModel model = aiModelMapper.selectById(id);
        if (model == null) throw new BusinessException(404, "AI模型不存在");
        if (name != null) model.setName(name);
        if (code != null) model.setCode(code);
        if (modelType != null) model.setModelType(modelType);
        if (icon != null) model.setIcon(icon);
        if (description != null) model.setDescription(description);
        if (sort != null) model.setSort(sort);
        if (status != null) model.setStatus(status);
        if (config != null) model.setConfig(config);
        if (defaultModel != null) model.setDefaultModel(defaultModel);
        if (apiConfigId != null) model.setApiConfigId(apiConfigId);
        aiModelMapper.updateById(model);
        chatModelFactory.evict(id);
        agentScopeModelFactory.evict(id);
    }

    @Transactional
    public void deleteAiModel(Long id) {
        aiModelMapper.deleteById(id);
        chatModelFactory.evict(id);
        agentScopeModelFactory.evict(id);
    }

    public AiModel getById(Long id) {
        return aiModelMapper.selectById(id);
    }

    public PageResult<AiModel> getPage(String name, String code, Integer modelType, Integer status,
                                        int pageNo, int pageSize) {
        LambdaQueryWrapper<AiModel> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(name != null, AiModel::getName, name)
                .like(code != null, AiModel::getCode, code)
                .eq(modelType != null, AiModel::getModelType, modelType)
                .eq(status != null, AiModel::getStatus, status)
                .orderByAsc(AiModel::getSort)
                .orderByDesc(AiModel::getId);
        return PageResult.of(aiModelMapper.selectPage(new Page<>(pageNo, pageSize), wrapper));
    }

    public List<AiModel> getEnabledList() {
        return aiModelMapper.selectList(new LambdaQueryWrapper<AiModel>().eq(AiModel::getStatus, 1));
    }

    public List<AiModel> getListByType(Integer modelType) {
        return aiModelMapper.selectList(new LambdaQueryWrapper<AiModel>()
                .eq(AiModel::getStatus, 1)
                .eq(AiModel::getModelType, modelType));
    }

    public AiModel getDefaultByType(Integer modelType) {
        return aiModelMapper.selectOne(new LambdaQueryWrapper<AiModel>()
                .eq(AiModel::getDefaultModel, true)
                .eq(AiModel::getModelType, modelType)
                .eq(AiModel::getStatus, 1)
                .orderByAsc(AiModel::getSort)
                .last("LIMIT 1"));
    }
}
