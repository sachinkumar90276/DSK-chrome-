package com.example.wallpaper

enum class WallpaperMediaType(val label: String) {
    DSK_LIVE("DSK Live"),
    PHOTO("Photo"),
    VIDEO("Video"),
    AI_IMAGE("AI Wallpaper"),
    AI_LIVE("AI Live Video"),
    STATIC_PRESET("Preset")
}

enum class WallpaperFitMode(val label: String) {
    FILL("Fill Screen"),
    FIT("Fit Inside"),
    CENTER("Center"),
    STRETCH("Stretch")
}

enum class DskLivePreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val styleTag: String
) {
    DSK_CHROME_FLOW(
        id = "dsk_live_chrome_flow",
        title = "DSK CHROME FLOW",
        subtitle = "Liquid metallic chrome, smooth reflections & sweeping light",
        styleTag = "Chrome"
    ),
    DSK_NEON_CYBER(
        id = "dsk_live_neon_cyber",
        title = "DSK NEON CYBER",
        subtitle = "Dark futuristic blue & purple neon particles & cyber beams",
        styleTag = "Cyberpunk"
    ),
    DSK_GALAXY_MOTION(
        id = "dsk_live_galaxy_motion",
        title = "DSK GALAXY MOTION",
        subtitle = "Deep nebula clouds, twinkling stars & orbiting planets",
        styleTag = "Space"
    ),
    DSK_RAINY_CITY(
        id = "dsk_live_rainy_city",
        title = "DSK RAINY CITY",
        subtitle = "Night skyline, glowing neon signs, moving rain & wet reflections",
        styleTag = "Cinematic"
    ),
    DSK_CHROME_SIGNATURE(
        id = "dsk_live_chrome_signature",
        title = "DSK CHROME SIGNATURE",
        subtitle = "Official DSK CHROME crown emblem with animated metallic rings",
        styleTag = "Signature"
    );

    companion object {
        fun fromIdOrNull(id: String): DskLivePreset? = entries.firstOrNull {
            it.id == id || it.name.equals(id, ignoreCase = true)
        }
    }
}

enum class AiWallpaperStyle(val label: String, val promptEnhancer: String) {
    REALISTIC("Realistic", "photorealistic, ultra detailed, crisp natural lighting, 8k wallpaper"),
    CINEMATIC("Cinematic", "cinematic wide composition, dramatic volumetric lighting, moody depth of field"),
    THREE_D("3D", "sleek 3D octane render, smooth sculpted geometry, studio rim lighting"),
    ANIME("Anime", "high-detail anime background art, rich atmospheric sky, vibrant cel lighting"),
    NATURE("Nature", "breathtaking landscape photography, pristine atmosphere, golden hour light"),
    CYBERPUNK("Cyberpunk", "futuristic cyberpunk metropolis at night, neon reflections on wet asphalt"),
    NEON("Neon", "glowing electric cyan and violet neon trails on deep obsidian backdrop"),
    MINIMAL("Minimal", "clean minimalist architectural composition, subtle gradients, refined negative space"),
    ABSTRACT("Abstract", "fluid sculpted abstract waves, metallic chrome and iridescent glass ribbons"),
    SPACE("Space", "deep space cosmic nebula, glowing stardust, distant ringed exoplanet")
}

enum class AiAspectRatio(val label: String, val ratioValue: String) {
    PORTRAIT_9_16("9:16", "9:16"),
    LANDSCAPE_16_9("16:9", "16:9"),
    SQUARE_1_1("1:1", "1:1"),
    PORTRAIT_3_4("3:4", "3:4")
}

enum class AiImageQuality(val label: String, val modelName: String) {
    FAST("Fast (1K)", "gemini-nano-banana-2.1"),
    STUDIO("Studio HD", "gemini-3-pro-image")
}

enum class WallpaperStudioSection(val label: String) {
    MY_WALLPAPERS("My Wallpapers"),
    PHOTO_WALLPAPERS("Photo Wallpapers"),
    VIDEO_WALLPAPERS("Video Wallpapers"),
    AI_WALLPAPERS("AI Wallpapers"),
    AI_LIVE_WALLPAPERS("AI Live Wallpapers"),
    DSK_LIVE_WALLPAPERS("DSK Live Wallpapers"),
    FAVORITES("Favorites"),
    RECENTLY_USED("Recently Used")
}

data class AiWallpaperSuggestion(
    val id: String,
    val title: String,
    val reason: String,
    val style: AiWallpaperStyle,
    val prompt: String,
    val linkedLivePreset: DskLivePreset? = null
)
