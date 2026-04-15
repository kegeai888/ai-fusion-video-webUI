package com.stonewu.fusion.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 * 预设画风注册表
 * <p>
 * 静态定义所有预设画风配置，包括名称、描述、英文提示词和参考图路径。
 * 预设参考图存放在 resources/static/art-styles/ 目录下。
 */
public class ArtStylePresets {

    private static final Map<String, ArtStylePreset> PRESETS = new LinkedHashMap<>();

    static {
        register(ArtStylePreset.builder()
                .key("cartoon_3d")
                .name("卡通3D")
                .description("卡通3D动画风格画面，圆润光滑角色造型，夸张可爱比例，明亮鲜艳糖果色调，柔和全局光照，塑料质感光泽表面")
                .imagePrompt("风格化3D卡通动画，圆润光滑的角色造型，夸张可爱的身体比例，明亮鲜艳的糖果色调，柔和的全局光照，塑料质感的光泽表面，温暖灯光下的简洁干净背景")
                .referenceImagePath("/art-styles/cartoon_3d.png")
                .build());

        register(ArtStylePreset.builder()
                .key("cg")
                .name("CG动画")
                .description("写实3D CG动画风格画面，高精度角色建模，真人化比例，皮肤次表面散射质感，电影级体积光照，物理材质渲染，景深虚化")
                .imagePrompt("写实3D CG动画，高面数精细角色建模，接近真人的身体比例，皮肤次表面散射质感，电影级体积光照，基于物理的材质渲染，景深虚化效果，精细的环境纹理")
                .referenceImagePath("/art-styles/cg.png")
                .build());

        register(ArtStylePreset.builder()
                .key("realistic")
                .name("写实")
                .description("电影级真人写实画面，自然柔光照明，细腻皮肤毛孔质感，浅景深虚化，电影调色与变形镜头光斑")
                .imagePrompt("电影级真人写实画面，自然柔和的灯光，8K超高清分辨率，细腻的皮肤毛孔纹理质感，真实的人体比例，浅景深虚化，电影级调色，变形镜头光斑效果")
                .referenceImagePath("/art-styles/realistic.png")
                .build());

        register(ArtStylePreset.builder()
                .key("anime_jp")
                .name("日漫")
                .description("日式赛璐璐动漫风格画面，平涂着色，清晰锐利描线，明亮柔和色调，精致手绘背景")
                .imagePrompt("日式赛璐璐动漫风格，平涂着色，修长纤细的动漫体型，头身比约1:6，大而有神的眼睛占面部三分之一，清晰锐利的轮廓线，柔和的环境光遮蔽阴影，鲜艳明亮的粉彩色调，精致手绘背景，网点纹理效果")
                .referenceImagePath("/art-styles/anime_jp.png")
                .build());

        register(ArtStylePreset.builder()
                .key("anime_cn")
                .name("国漫")
                .description("中国水墨动漫风格画面，流畅水墨笔触线条，泼墨粒子效果，朱红翠绿传统色调，高对比戏剧光影")
                .imagePrompt("中国风动漫画面，流畅灵动的水墨笔触线条，大胆夸张的动作姿势，修长矫健的身体比例，浓郁饱和的中国传统色调搭配朱红与翠绿，泼墨飞溅的粒子效果，高对比度戏剧性光影")
                .referenceImagePath("/art-styles/anime_cn.png")
                .build());

        register(ArtStylePreset.builder()
                .key("comic_us")
                .name("美漫")
                .description("美式漫画风格画面，粗重描边，交叉阴影线，高饱和原色，夸张透视前缩，网点纹理，对角线动态构图")
                .imagePrompt("美式漫画风格，粗重大胆的轮廓线，交叉排线阴影，高饱和度原色调，夸张的透视前缩效果，肌肉感的风格化人体造型，网点纹理，对角线动态构图")
                .referenceImagePath("/art-styles/comic_us.png")
                .build());
    }

    private static void register(ArtStylePreset preset) {
        PRESETS.put(preset.getKey(), preset);
    }

    /**
     * 根据 key 获取预设画风
     */
    public static ArtStylePreset getByKey(String key) {
        return PRESETS.get(key);
    }

    /**
     * 获取所有预设画风列表
     */
    public static List<ArtStylePreset> getAll() {
        return new ArrayList<>(PRESETS.values());
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ArtStylePreset {
        /** 画风唯一标识 */
        private String key;
        /** 画风显示名称 */
        private String name;
        /** 画风描述（中文，用于视频生成 prompt 前缀） */
        private String description;
        /** 画风英文提示词（用于图片生成） */
        private String imagePrompt;
        /** 参考图相对路径（如 /art-styles/cg.png） */
        private String referenceImagePath;
        /** 参考图公网 URL（上传到 OSS 后由系统配置填充） */
        private String referenceImagePublicUrl;
    }
}
