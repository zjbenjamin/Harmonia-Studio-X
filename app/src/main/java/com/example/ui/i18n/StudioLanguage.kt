package com.example.ui.i18n

enum class AppLanguage(val code: String, val displayName: String) {
    SIMPLIFIED_CHINESE("zh", "简体中文"),
    ENGLISH("en", "English")
}

data class StudioStrings(
    // App & Nav
    val appTitle: String,
    val tabArranger: String,
    val tabPianoRoll: String,
    val tabBeatSequencer: String,
    val tabController: String,
    val tabSoundLibrary: String,
    val tabCollab: String,

    // Transport
    val play: String,
    val pause: String,
    val stop: String,
    val record: String,
    val loop: String,
    val metronome: String,
    val masterVolume: String,
    val bpmDialogTitle: String,
    val tempoLabel: String,
    val apply: String,
    val cancel: String,
    val exportAudio: String,

    // Arranger
    val addTrack: String,
    val smartChords: String,
    val drumMachine: String,
    val muteShort: String,
    val soloShort: String,
    val tracksCount: String,
    val bar: String,
    val addTrackTitle: String,

    // Chord Generator
    val chordGeneratorTitle: String,
    val chordGeneratorSubtitle: String,
    val sectionKeyScale: String,
    val sectionGenre: String,
    val sectionMood: String,
    val sectionComplexityVoicing: String,
    val sectionSequence: String,
    val applyChordsToTrack: String,
    val audition: String,

    // Beat Sequencer
    val beatSequencerTitle: String,
    val presets: String,
    val clear: String,
    val swingGroove: String,
    val applyBeatsToTimeline: String,
    val stepVelocityTitle: String,
    val selectPresetGroove: String,
    val done: String,

    // Controller
    val modeKeys: String,
    val modeDrumPads: String,
    val octave: String,
    val sustain: String,
    val trackLabel: String,
    val recLiveMidi: String,

    // Sound Library
    val synthEngine: String,
    val useOnTrack: String,
    val attack: String,
    val release: String,
    val filterCutoff: String,
    val reverb: String,

    // Audio Export
    val exportDialogTitle: String,
    val selectFormat: String,
    val masterOptions: String,
    val peakNormalization: String,
    val peakNormalizationDesc: String,
    val reverbTail: String,
    val reverbTailDesc: String,
    val renderExportButton: String,
    val previewMaster: String,
    val shareAudio: String,
    val renderComplete: String,

    // Collab & Sync
    val cloudWorkspaceTitle: String,
    val statusLabel: String,
    val syncNow: String,
    val projectShareCode: String,
    val copyCode: String,
    val activeCollaborators: String,
    val crossPlatformInterchange: String,
    val exportMidiFile: String,
    val renderAudioFile: String,
    val exportProjectJson: String,
    val importProjectJson: String,
    val revisionHistory: String,
    val languageSetting: String
)

object StudioI18n {

    val CHINESE = StudioStrings(
        appTitle = "Harmonia 音乐编曲工作站",
        tabArranger = "编曲工作台",
        tabPianoRoll = "钢琴卷帘",
        tabBeatSequencer = "节奏鼓机",
        tabController = "虚拟琴键",
        tabSoundLibrary = "音色库",
        tabCollab = "多端协同与同步",

        play = "播放",
        pause = "暂停",
        stop = "停止",
        record = "录音",
        loop = "循环",
        metronome = "节拍器",
        masterVolume = "主音量",
        bpmDialogTitle = "曲速与拍号设置",
        tempoLabel = "速度 (BPM)",
        apply = "应用",
        cancel = "取消",
        exportAudio = "导出音频",

        addTrack = "添加音轨",
        smartChords = "智能和弦",
        drumMachine = "节奏鼓机",
        muteShort = "静",
        soloShort = "独",
        tracksCount = "音轨列表",
        bar = "小节",
        addTrackTitle = "添加乐器音轨",

        chordGeneratorTitle = "智能和弦进行生成器",
        chordGeneratorSubtitle = "基于调性、曲风流派与情绪氛围的 AI 和声编配引擎",
        sectionKeyScale = "1. 调性与音阶选择",
        sectionGenre = "2. 音乐曲风流派",
        sectionMood = "3. 情绪氛围定制",
        sectionComplexityVoicing = "4. 和声复杂度与织体模式",
        sectionSequence = "生成和弦序列 (点击卡片实时试听)",
        applyChordsToTrack = "将和弦进行应用至当前轨道",
        audition = "试听",

        beatSequencerTitle = "高级 16 步节奏音序器",
        presets = "经典预设",
        clear = "清空",
        swingGroove = "摇摆律动 (Swing)",
        applyBeatsToTimeline = "将鼓组律动应用至时间轴",
        stepVelocityTitle = "步进力度调节",
        selectPresetGroove = "选择律动风格预设",
        done = "完成",

        modeKeys = "琴键键盘",
        modeDrumPads = "16格打击垫",
        octave = "八度",
        sustain = "延音踏板",
        trackLabel = "当前轨道",
        recLiveMidi = "● 实时 MIDI 录制中",

        synthEngine = "合成器机架与包络",
        useOnTrack = "应用到当前轨道",
        attack = "起音时间 (Attack)",
        release = "释音时间 (Release)",
        filterCutoff = "滤波截止频率 (Cutoff)",
        reverb = "混响深度 (Reverb)",

        exportDialogTitle = "导出工程音频文件",
        selectFormat = "1. 选择导出音频格式",
        masterOptions = "2. 母带后期处理选项",
        peakNormalization = "峰值标准化 (-0.3 dBFS)",
        peakNormalizationDesc = "智能最大化母带音量动态，防止数字削波失真",
        reverbTail = "自然混响残响尾音 (+2.0秒)",
        reverbTailDesc = "保留合成器长音、延音及镲片自然衰减残响",
        renderExportButton = "开始渲染并导出音频",
        previewMaster = "应用内试听母带音频",
        shareAudio = "分享音频文件",
        renderComplete = "音频渲染完成！",

        cloudWorkspaceTitle = "云端工作区与多设备无缝同步",
        statusLabel = "同步状态",
        syncNow = "立即同步",
        projectShareCode = "工程跨端同步分享码",
        copyCode = "复制分享码",
        activeCollaborators = "在线协同成员与设备",
        crossPlatformInterchange = "跨平台工程交换与母带导出",
        exportMidiFile = "导出标准 MIDI (.mid)",
        renderAudioFile = "渲染工程音频 (.wav)",
        exportProjectJson = "导出工程数据包 (JSON)",
        importProjectJson = "导入外部工程 (JSON)",
        revisionHistory = "历史修订记录与操作日志",
        languageSetting = "语言设置 (Language)"
    )

    val ENGLISH = StudioStrings(
        appTitle = "Harmonia Studio DAW",
        tabArranger = "Arranger",
        tabPianoRoll = "Piano Roll",
        tabBeatSequencer = "Beat Sequencer",
        tabController = "Virtual Keys",
        tabSoundLibrary = "Sounds",
        tabCollab = "Sync & Collab",

        play = "Play",
        pause = "Pause",
        stop = "Stop",
        record = "Record",
        loop = "Loop",
        metronome = "Metronome",
        masterVolume = "Master Volume",
        bpmDialogTitle = "Tempo & Time Signature",
        tempoLabel = "Tempo (BPM)",
        apply = "Apply",
        cancel = "Cancel",
        exportAudio = "Export Audio",

        addTrack = "Add Track",
        smartChords = "Smart Chords",
        drumMachine = "Drum Machine",
        muteShort = "M",
        soloShort = "S",
        tracksCount = "Tracks",
        bar = "Bar",
        addTrackTitle = "Add Instrument Track",

        chordGeneratorTitle = "Smart Chord Progression Generator",
        chordGeneratorSubtitle = "AI-assisted harmonic progressions based on key, genre & mood",
        sectionKeyScale = "1. MUSICAL KEY & SCALE",
        sectionGenre = "2. GENRE STYLE",
        sectionMood = "3. EMOTIONAL MOOD",
        sectionComplexityVoicing = "4. HARMONIC COMPLEXITY & VOICING",
        sectionSequence = "GENERATED CHORD SEQUENCE (TAP TO AUDITION)",
        applyChordsToTrack = "Apply Progression to Track",
        audition = "Audition",

        beatSequencerTitle = "Advanced 16-Step Beat Sequencer",
        presets = "Presets",
        clear = "Clear",
        swingGroove = "Swing & Groove",
        applyBeatsToTimeline = "Apply Drum Pattern to Timeline",
        stepVelocityTitle = "Step Velocity Accent",
        selectPresetGroove = "Select Groove Preset",
        done = "Done",

        modeKeys = "Keys",
        modeDrumPads = "16 Drum Pads",
        octave = "Octave",
        sustain = "Sustain",
        trackLabel = "Track",
        recLiveMidi = "● REC LIVE MIDI",

        synthEngine = "SYNTH ENGINE & ENVELOPE",
        useOnTrack = "Use on Track",
        attack = "Attack",
        release = "Release",
        filterCutoff = "Filter Cutoff",
        reverb = "Reverb Send",

        exportDialogTitle = "Export Project Audio",
        selectFormat = "1. SELECT EXPORT FORMAT",
        masterOptions = "2. MASTER PROCESSING OPTIONS",
        peakNormalization = "Peak Normalization (-0.3 dBFS)",
        peakNormalizationDesc = "Maximizes dynamic loudness without digital clipping",
        reverbTail = "Include Natural Reverb Tail (+2.0s)",
        reverbTailDesc = "Prevents abrupt ending cuts for decaying synths and cymbals",
        renderExportButton = "Render & Export Audio",
        previewMaster = "Preview Master Audio",
        shareAudio = "Share Audio",
        renderComplete = "Render Complete!",

        cloudWorkspaceTitle = "Cloud Workspace & Multi-Device Sync",
        statusLabel = "Status",
        syncNow = "Sync Now",
        projectShareCode = "PROJECT SHARE CODE",
        copyCode = "Copy Code",
        activeCollaborators = "ACTIVE COLLABORATORS",
        crossPlatformInterchange = "CROSS-PLATFORM EXPORT & IMPORT",
        exportMidiFile = "Export MIDI (.mid)",
        renderAudioFile = "Render Audio (.wav)",
        exportProjectJson = "Export Project JSON",
        importProjectJson = "Import Project JSON",
        revisionHistory = "REVISION HISTORY & LOG",
        languageSetting = "Language Settings"
    )

    fun getStrings(language: AppLanguage): StudioStrings {
        return when (language) {
            AppLanguage.SIMPLIFIED_CHINESE -> CHINESE
            AppLanguage.ENGLISH -> ENGLISH
        }
    }
}
