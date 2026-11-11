package mindustryX.bundles

import arc.util.Strings
import mindustryX.VarsX

/** Vietnamese UI text.  Keep the protocol markers and interpolation values intact. */
internal object UiTextBundleVi : UiTextBundle {
    override fun mdtxReport() = "Báo lỗi"
    override fun mdtxQqLink() = "Nhóm QQ"
    override fun modsRecommendTitle() = "[accent]MdtX[] - Danh sách mod đề xuất"
    override fun modsRecommendInfo() = "Mod hỗ trợ được tuyển chọn"
    override fun modsRecommendLastUpdated(value: String) = "Cập nhật danh sách đề xuất lần cuối: $value"
    override fun modsRecommendModName(value: String) = "Mod: $value"
    override fun modsRecommendModAuthor(value: String) = "Tác giả: $value"
    override fun modsRecommendModMinGameVersion(value: String) = "Phiên bản trò chơi hỗ trợ tối thiểu: $value"
    override fun modsRecommendModLastUpdated(value: String) = "Cập nhật lần cuối: $value"
    override fun modsRecommendModStars(value: String) = "Số sao GitHub: $value"
    override fun mdtxShareItem(name: String, stock: String, production: String) = "$name: Tồn kho $stock, sản lượng $production/giây"
    override fun mdtxShareUnit(name: String, count: String, limit: Int) = "$name: Số lượng $count, giới hạn $limit"
    override fun itemSelectionHeight(value: Int) = "$value hàng"
    override fun itemSelectionWidth(value: Int) = "$value cột"
    override fun javaWarnLog(javaVersion: String) = "Phiên bản Java $javaVersion quá thấp và không được hỗ trợ. Hãy dùng Java 17 trở lên để chạy MindustryX."
    override fun javaWarnDialog(javaVersion: String) = "Phiên bản Java $javaVersion quá thấp và không được hỗ trợ.\n[grey]Không thể tắt cảnh báo này; hãy cập nhật Java."
    override fun windowTitle(version: String, enabledMods: Int, totalMods: Int, width: Int, height: Int) = "MindustryX | Phiên bản $version | mod đã bật $enabledMods/$totalMods | ${width}x$height"
    override fun currentVersion(version: String) = "Phiên bản hiện tại: $version"
    override fun newVersion(version: String) = "[green]Đã tìm thấy phiên bản mới[]: $version"
    override fun loadMap(mapName: String) = "Tải bản đồ: $mapName"
    override fun introduction(description: String) = "Giới thiệu: $description"
    override fun waveEvent(wave: Int, detail: String) = "Đợt: $wave | $detail"
    override fun exportHeader(version: String) = "[MDTX-$version] Lịch sử trò chuyện trong trò chơi đã xuất"
    override fun exportMap(mapName: String, mode: String) = "*** Tên bản đồ hiện tại: $mapName (chế độ: $mode)\n"
    override fun currentWave(wave: Int) = "*** Đợt hiện tại: $wave"
    override fun exportCount(count: Int) = "Đã chọn thành công tổng cộng $count bản ghi:\n"
    override fun toggleState(label: String, state: String) = "$label: $state"
    override fun copiedMemory(value: Double) = "[cyan]Đã sao chép bộ nhớ[white]\n $value"
    override fun copiedVariableNameHint(value: String) = "Đã sao chép tên biến\n$value"
    override fun copiedVariableAttributesHint(value: String) = "Đã sao chép thuộc tính biến\n$value"
    override fun copiedPrintBufferHint(value: String) = "Đã sao chép bộ đệm in\n$value"
    override fun failedReadImage(error: Throwable) = "Không thể đọc ảnh, hãy thử ảnh khác\n${error.message ?: error}"
    override fun invalidBackgroundImage(path: String) = "Ảnh nền không hợp lệ: $path"
    override fun labelWithEmoji(label: String, emoji: String) = "$label $emoji"
    override fun savedBlueprint(name: String) = "Đã lưu bản thiết kế: $name"
    override fun fpsLockOff(gameSpeed: Float) = "Đã tắt khóa tốc độ khung hình\nTốc độ trò chơi hiện tại: $gameSpeed lần"
    override fun currentGameSpeed(gameSpeed: Float) = "Tốc độ trò chơi hiện tại: $gameSpeed lần"
    override fun fpsLockEnabled(targetFps: Int) = "Đã bật khóa tốc độ khung hình\nMức khóa hiện tại: $targetFps"
    override fun fpsLockCurrent(targetFps: Int) = "Mức tốc độ khung hình đang khóa: $targetFps"
    override fun currentMap(mapName: String) = "Bản đồ hiện tại: $mapName"
    override fun worldProcessorSummary(processors: Int, instructions: Int, chars: Int) = "Bản đồ có $processors bộ xử lý thế giới, $instructions dòng chỉ lệnh và $chars ký tự."
    override fun recording(path: String) = "Đang ghi: $path"
    override fun coordinateDistance(x: Int, y: Int, distance: Int) = "$x,$y\nKhoảng cách: $distance"
    override fun atPlayer(playerName: String?) = "<AT> đã nhắc ${playerName ?: ""}[white] kiểm tra tin nhắn."
    override fun atNoticeFrom(senderName: String) = "[gold]Bạn được [white]$senderName[gold] nhắc! Hãy kiểm tra hộp thoại tin nhắn."
    override fun shareSchematic(name: String, playerName: String?, requirements: String, powerInfo: String, link: String?, code: String) = buildString {
        appendLine("Đây là bản thiết kế được chia sẻ từ MDTX-${VarsX.version}")
        appendLine("Tên bản thiết kế: $name")
        appendLine("Người chia sẻ: ${playerName ?: ""}")
        appendLine("Chi phí bản thiết kế: $requirements")
        appendLine("Điện năng: $powerInfo")
        appendLine("Liên kết mã bản thiết kế: ${link ?: "x"}")
        if (code.length < 3500) {
            append("Mã bản thiết kế:\n").append(code)
        } else {
            append("Mã bản thiết kế quá dài, hãy mở liên kết để xem")
        }
    }
    override fun waveContains(ground: Int, air: Int) = "Gồm (mặt đất x$ground, trên không x$air):"
    override fun waveTitle(wave: Int) = "Đợt $wave"
    override fun waveEta(remainingWaves: Int, eta: String) = "(còn $remainingWaves đợt, $eta)"
    override fun shieldCapacity(max: String, radius: String, recovery: String, cooldown: String) = "[lightgray]${max} độ bền khiên[accent]~[]${radius} ô[accent]~[]${recovery} hồi phục[accent]~[]${cooldown}s thời gian hồi phục"
    override fun liquidExplode(total: String, liquidName: String, liquidEmoji: String, radius: String) = "[lightgray]Tổng ${total}${liquidName}${liquidEmoji}[accent]~[]bán kính $radius ô"
    override fun liquidRegen(absorb: String, liquidName: String, liquidEmoji: String, heal: String, maxHeal: String) = "[lightgray]Mỗi ô hấp thụ ${absorb}/s${liquidName}${liquidEmoji}[accent]~[]hồi ${heal}/s[accent]~[]tối đa ${maxHeal}/s"
    override fun lightning(probability: String, damage: String, length: String, speed: String) = "[lightgray]Phóng điện: xác suất $probability[accent]~[]sát thương $damage[accent]~[]độ dài $length, tốc độ ${speed}x"
    override fun durationTiles(seconds: String, tiles: String) = "[lightgray]${seconds}s[accent]~[]$tiles ô"
    override fun refreshInterval(value: Int) = "Khoảng thời gian làm mới $value"
    override fun zoomScale(scale: String) = "Thu phóng: x$scale"
    override fun teamRange(teamId: Int) = "Đội: $teamId~${teamId + 9}"
    override fun copiedSuccessfully(text: String) = "Đã sao chép thành công: $text"
    override fun builtInDisplayName(name: String) = "[Tích hợp]$name"
    override fun playbackVersion(value: String) = "Phiên bản phát lại: $value"
    override fun replayCreationTime(value: String) = "Thời gian tạo bản phát lại: $value"
    override fun serverIp(value: String) = "IP máy chủ: $value"
    override fun playerName(value: String) = "Tên người chơi: $value"
    override fun packetCount(value: Int) = "Số gói tin: $value"
    override fun playbackLength(value: String) = "Thời lượng bản phát lại: $value"
    override fun sizeWithDimensions(width: String, height: String) = "Kích thước: $width\uE815$height"
    override fun tiles(value: Number) = "$value ô"
    override fun tilesOrOff(value: Int) = if (value > 0) "$value ô" else "Tắt"
    override fun percentOrOff(value: Int) = if (value > 0) "$value%" else "Tắt"
    override fun hpOrAll(value: Int) = if (value > 0) "$value[red]HP" else "Hiện tất cả"
    override fun radarSpeedMode(value: Int) = when (value) {
        0 -> "Tắt"
        30 -> "Tức thì"
        else -> "[lightgray]x[white]" + Strings.autoFixed(value * 0.2f, 1) + "x tốc độ quét"
    }
    override fun radarSizeMode(value: Int) = if (value == 0) "Kích thước cố định" else "[lightgray]x[white]" + Strings.autoFixed(value * 0.1f, 1) + "x"
    override fun turretShowRangeMode(value: Int) = when (value) {
        0 -> "Tắt"
        1 -> "Chỉ mặt đất"
        2 -> "Chỉ trên không"
        3 -> "Tất cả"
        else -> ""
    }

    override fun unitWeaponRangeMode(value: Int) = when (value) {
        0 -> "Tắt"
        30 -> "Luôn bật"
        else -> "$value ô"
    }

    override fun unitTargetTypeMode(value: Int) = when (value) {
        0 -> "Tắt"
        1 -> "Vòng"
        2 -> "Tấn công"
        3 -> "Tấn công (không viền)"
        4 -> "Vòng chữ thập"
        5 -> "Chữ thập"
        else -> value.toString()
    }

    override fun superUnitEffectMode(value: Int) = when (value) {
        0 -> "Tắt"
        1 -> "Duy nhất"
        2 -> "Tất cả người chơi"
        else -> value.toString()
    }
    override fun kotlinModName() = "Thư viện chuẩn Kotlin"
    override fun confirmDeleteFile(filename: String) = "Xác nhận xóa tệp?\n$filename"
    override fun fileNotFound(filename: String) = "Không tìm thấy tệp: $filename"

    override fun simple(key: String): String = translations[key] ?: key
    override val labelsResFile: String get() = "labels_vi"

    private val translations = hashMapOf(
        "\n[white]分走了单位:" to "\n[white] đã lấy các đơn vị:",
        "<永久状态>" to "<Trạng thái vĩnh viễn>",
        "<瞬间状态>" to "<Trạng thái tức thời>",
        ">> 雷达扫描中 <<" to ">> Đang quét radar <<",
        "ARC-AI设定器" to "Bộ cấu hình ARC-AI",
        "ARC-中央监控室" to "Trung tâm tin nhắn ARC",
        "ARC-矿物统计" to "Thống kê quặng ARC",
        "Kotlin语言标准库" to "Thư viện chuẩn Kotlin",
        "TIP: 所有包含处理器的蓝图" to "MẸO: Mọi bản thiết kế có bộ xử lý",
        "UI图标大全" to "Thư viện biểu tượng UI",
        "[accent]建速" to "[accent]Tốc độ xây",
        "[acid]血量" to "[acid]Độ bền",
        "[cyan]标记模式,点击屏幕标记." to "[cyan]Chế độ đánh dấu: chạm màn hình để đánh dấu.",
        "[cyan]移速" to "[cyan]Tốc độ di chuyển",
        "[green]点击屏幕返回" to "[green]Chạm màn hình để quay lại",
        "[green]点击屏幕采集坐标" to "[green]Chạm màn hình để lấy tọa độ",
        "[orange]你被戳了一下，请注意查看信息框哦~" to "[orange]Bạn vừa được nhắc. Hãy kiểm tra hộp thoại tin nhắn.",
        "[orange]已更新编辑的逻辑！" to "[orange]Đã cập nhật logic đang sửa!",
        "[orange]生成的单位会飞起来" to "[orange]đơn vị tạo ra sẽ bay",
        "[orange]生成！" to "[orange]đã tạo!",
        "[orange]警告：图片可能过大，请尝试压缩图片" to "[orange]Cảnh báo: ảnh có thể quá lớn, hãy thử nén ảnh",
        "[purple]阻力" to "[purple]Lực cản",
        "[red]伤害" to "[red]Sát thương",
        "[red]当前单位不可建筑" to "[red]Đơn vị hiện tại không thể xây dựng",
        "[red]血量：" to "[red]Độ bền:",
        "[red]这是一个作弊功能[]\n快速跳转到目标波次(不刷兵)" to "[red]Đây là tính năng gian lận[]\nĐến ngay đợt mục tiêu (không tạo quân địch)",
        "[teal]装甲" to "[teal]Giáp",
        "[violet]攻速" to "[violet]Tốc độ tấn công",
        "[white]法" to "[white]S",
        "[yellow]建筑过多，避免卡顿，仅保留前1000个规划" to "[yellow]Quá nhiều khối; chỉ giữ 1000 kế hoạch đầu để tránh giật.",
        "[yellow]当前无权编辑，仅供查阅" to "[yellow]Bạn không có quyền chỉnh sửa; chỉ xem.",
        "[yellow]当前选中物品为空，请在物品栏选中建筑" to "[yellow]Chưa chọn gì; hãy chọn khối trong thanh chọn.",
        "[yellow]护盾：" to "[yellow]Khiên:",
        "[yellow]添加新指令前，请先保存编辑的指令" to "[yellow]Hãy lưu lệnh đang sửa trước khi thêm lệnh mới.",
        "arc-图片转换器" to "Bộ chuyển đổi ảnh ARC",
        "minerAI-矿物筛选器" to "Miner AI - Bộ lọc quặng",
        "ui大全" to "Bộ công cụ UI",
        "一键装填" to "Nạp tự động",
        "上传失败，再重试一下？" to "Tải lên thất bại, thử lại nhé?",
        "丢下载荷" to "Thả khối hàng",
        "中央监控室" to "Trung tâm tin nhắn",
        "事件~波次" to "Sự kiện~Đợt",
        "事件~载入地图" to "Sự kiện~Tải bản đồ",
        "从蓝图中选择代码" to "Lấy mã từ bản thiết kế",
        "你已是最新版本，不需要更新！" to "Bạn đang dùng phiên bản mới nhất.",
        "保护AI" to "AI phòng thủ",
        "信" to "M",
        "信息板全显示" to "Hiện mọi bảng tin",
        "修复AI" to "AI sửa chữa",
        "像素画" to "Pixel art",
        "允许的范围：2~9999" to "Phạm vi cho phép: 2~9999",
        "克隆" to "Nhân bản",
        "全局检查" to "Phạm vi toàn cục",
        "全部显示" to "Hiện tất cả",
        "关闭" to "Tắt",
        "兵" to "U",
        "兵种显示" to "Hiển thị đơn vị",
        "分享单位数量" to "Chia sẻ số lượng đơn vị",
        "分享库存情况" to "Chia sẻ kho",
        "分享波次信息" to "Chia sẻ thông tin đợt",
        "分享电力情况" to "Chia sẻ điện năng",
        "分享者：" to "Người chia sẻ:",
        "创世神" to "Chế độ toàn năng",
        "创建回放出错!" to "Tạo bản phát lại thất bại!",
        "前缀添加/t" to "Thêm tiền tố /t",
        "加载回放文件" to "Tải tệp bản phát lại",
        "单位工厂" to "Nhà máy đơn vị",
        "单位工厂-X" to "Nhà máy đơn vị-X",
        "单位：" to "Đơn vị:",
        "原始大小" to "Kích thước gốc",
        "原版" to "Nguyên bản",
        "发布说明" to "Ghi chú phát hành",
        "受不了，直接投降？" to "Bạn chắc muốn đầu hàng?",
        "变动闪烁" to "Nhấp nháy khi thay đổi",
        "变量变动闪烁" to "Nhấp nháy khi biến đổi",
        "变量自动更新" to "Tự làm mới biến",
        "只显示建筑状态" to "Chỉ hiện trạng thái khối",
        "同步一波" to "Đồng bộ một đợt",
        "名称" to "Tên",
        "回放统计" to "Thống kê bản phát lại",
        "回放文件不存在" to "Không tìm thấy tệp bản phát lại",
        "回放管理器" to "Trình quản lý bản phát lại",
        "位置" to "Vị trí",
        "录制者" to "Người ghi",
        "创建时间" to "Đã tạo",
        "类型" to "Loại",
        "次数" to "Số lần",
        "字节" to "Byte",
        "停止回放" to "Dừng bản phát lại",
        "输入无效" to "Dữ liệu nhập không hợp lệ",
        "跳转" to "Đến",
        "跳转到(秒)" to "Đến (giây)",
        "加载外部回放" to "Tải bản phát lại ngoài",
        "搜索回放" to "Tìm bản phát lại",
        "没有可管理的回放文件" to "Không có tệp bản phát lại để quản lý",
        "没有匹配的回放文件" to "Không có tệp bản phát lại phù hợp",
        "读取回放头信息中..." to "Đang đọc đầu bản phát lại...",
        "无法读取回放头信息" to "Không thể đọc đầu bản phát lại",
        "修改时间" to "Đã sửa",
        "文件大小" to "Kích thước",
        "录制时间" to "Đã ghi",
        "玩家" to "Người chơi",
        "服务器" to "Máy chủ",
        "版本" to "Phiên bản",
        "确认删除回放文件?" to "Xóa tệp bản phát lại?",
        "删除回放文件失败" to "Xóa tệp bản phát lại thất bại",
        "图标" to "Biểu tượng",
        "在建造列表加入被摧毁建筑" to "Thêm khối bị phá vào hàng xây",
        "在目标点附近的这个范围内随机生成" to "Tạo ngẫu nhiên trong phạm vi gần mục tiêu",
        "地形蓝图" to "Bản thiết kế địa hình",
        "块" to "B",
        "基础对比" to "Cơ bản",
        "填满核心的所有资源" to "Đổ đầy tài nguyên lõi",
        "墙" to "W",
        "墙体阴影显示" to "Hiển thị bóng tường",
        "子弹显示" to "Hiển thị đạn",
        "导出" to "Xuất",
        "导出聊天记录" to "Xuất lịch sử trò chuyện",
        "将时间流速加快到两倍" to "Tăng tốc thời gian lên 2 lần",
        "将时间流速放慢到一半" to "Giảm tốc thời gian còn một nửa",
        "小地图显示" to "Bản đồ nhỏ",
        "已保存至剪贴板" to "Đã lưu vào bộ nhớ tạm",
        "已导出本条聊天记录" to "Đã sao chép cuộc trò chuyện này",
        "已暂停" to "Đã tạm dừng",
        "已移除逻辑视角锁定" to "Đã bỏ khóa camera logic",
        "已继续游戏" to "Đã tiếp tục trò chơi",
        "帧率模拟" to "Mô phỏng tốc độ khung hình",
        "平方对比" to "Bình phương",
        "序号" to "Chỉ mục",
        "建筑显示" to "Hiển thị khối",
        "建筑：" to "Khối:",
        "建造区域" to "Phạm vi xây dựng",
        "开关" to "Bật/tắt",
        "开关自己队的无限火力" to "Bật gian lận cho đội mình",
        "开启" to "Bật",
        "弹" to "P",
        "强制助推" to "Ép tăng tốc",
        "强制跳波" to "Ép bỏ qua đợt",
        "当前不可用的面板:" to "Các bảng hiện không khả dụng:",
        "当前选定区域为空，请通过F规划区域" to "Vùng đang chọn trống. Hãy dùng F để quy hoạch.",
        "当前单位不能克隆：会生成非法单位并导致崩溃" to "Không thể nhân bản đơn vị hiện tại: đơn vị tạo ra sẽ không hợp lệ và làm trò chơi bị văng.",
        "录制出错!" to "Lỗi ghi!",
        "录制结束" to "Đã kết thúc ghi",
        "恢复原速" to "Trở lại tốc độ gốc",
        "恢复当前波次" to "Khôi phục đợt hiện tại",
        "战争迷雾" to "Sương mù chiến tranh",
        "打开发布页面" to "Mở trang phát hành",
        "打开回放文件" to "Mở tệp bản phát lại",
        "扫" to "S",
        "扫描模式" to "Chế độ quét",
        "指令" to "Lệnh",
        "指挥模式" to "Chế độ chỉ huy",
        "捡起载荷" to "Nhặt khối hàng",
        "携带物品:" to "Vật phẩm mang theo:",
        "放置/替换" to "Đặt/Thay thế",
        "效" to "FX",
        "无限制" to "Không giới hạn",
        "无限火力" to "Gian lận",
        "显示名" to "Tên hiển thị",
        "显示并允许建造所有物品" to "Hiển thị và cho phép xây mọi khối",
        "暂停建造" to "Đã tạm dừng xây",
        "暂停时间" to "Tạm dừng thời gian",
        "暂停逻辑(游戏)运行" to "Tạm dừng logic (trò chơi)",
        "暂存区" to "Khu tạm",
        "更多队伍选择" to "Thêm lựa chọn đội",
        "更新编辑的逻辑" to "Làm mới logic đang sửa",
        "最大储存聊天记录(过高可能导致卡顿)：" to "Số lịch sử trò chuyện tối đa (quá cao có thể gây giật):",
        "服务器信息" to "Thông báo máy chủ",
        "服务器信息版" to "Bản thông tin máy chủ",
        "未加载回放!" to "Chưa tải bản phát lại!",
        "未输入指令" to "Chưa nhập lệnh",
        "查找方块" to "Tìm công trình",
        "查看录制信息" to "Xem thông tin ghi",
        "标记~坐标" to "Đánh dấu~Tọa độ",
        "标记~玩家" to "Đánh dấu~Người chơi",
        "标记当前任务" to "Đánh dấu nhiệm vụ",
        "标记地图位置" to "Đánh dấu vị trí bản đồ",
        "格" to "ô",
        "检查更新失败，请稍后再试" to "Không thể kiểm tra cập nhật.\nHãy thử lại sau.",
        "未找到适用于当前平台的更新包" to "Không có gói cập nhật cho nền tảng hiện tại.",
        "正式版" to "Bản phát hành ổn định",
        "沙漏：" to "Đồng hồ cát:",
        "沙盒" to "Chế độ tự do",
        "法国军礼" to "Bình chọn đầu hàng",
        "波次信息" to "Thông tin đợt",
        "波次设定" to "Thiết lập đợt",
        "消息(@js 开头为脚本)" to "Tin nhắn (@js ở đầu là script)",
        "消息超过聊天长度限制" to "Tin nhắn vượt quá giới hạn độ dài trò chuyện",
        "液体" to "Chất lỏng",
        "添加队伍" to "Thêm đội",
        "添加面板" to "Thêm bảng",
        "清空" to "Xóa hết",
        "清空核心的所有资源" to "Xóa mọi tài nguyên lõi",
        "版" to "V",
        "物品" to "Vật phẩm",
        "特效大全" to "Thư viện hiệu ứng",
        "特效显示" to "Hiển thị hiệu ứng",
        "玩家建造区" to "Phạm vi xây của người chơi",
        "生成位置:" to "Vị trí tạo:",
        "生成数量:" to "Số lượng tạo:",
        "生成范围：" to "Phạm vi tạo:",
        "朝向" to "Hướng:",
        "时间线" to "Dòng thời gian",
        "生成队伍:" to "Đội tạo:",
        "电力：" to "Điện năng:",
        "画板" to "Bảng vẽ",
        "画板++" to "Bảng vẽ++",
        "瞬间完成" to "Tức thì",
        "矿机AI" to "AI đào",
        "矿物信息" to "Thông tin quặng",
        "矿物矿(地表/墙矿)" to "Số quặng (bề mặt/tường)",
        "碰撞箱显示" to "Hiện hộp va chạm",
        "秒" to "giây",
        "移除逻辑锁定" to "Bỏ khóa logic",
        "箱" to "H",
        "编辑器" to "Trình chỉnh sửa",
        "缩放" to "Thu phóng",
        "缩放: " to "Thu phóng: ",
        "缩放: \uE815 " to "Thu phóng: \uE815 ",
        "缩放后大小" to "Kích thước sau thu phóng",
        "聊天" to "Trò chuyện",
        "自动下载更新" to "Tải và cài cập nhật tự động",
        "自动保存为蓝图" to "Tự động lưu thành bản thiết kế",
        "自动刷新变量" to "Tự động làm mới biến",
        "自动攻击" to "Tự động tấn công",
        "自动更新" to "Kiểm tra cập nhật",
        "自杀" to "Tự hủy",
        "色调函数:" to "Chế độ màu:",
        "蓝图代码过长，请点击链接查看" to "Mã bản thiết kế quá dài, hãy mở liên kết để xem",
        "蓝图代码链接：" to "Liên kết mã bản thiết kế:",
        "蓝图代码：\n" to "Mã bản thiết kế: \n",
        "蓝图名：" to "Tên bản thiết kế:",
        "蓝图造价：" to "Chi phí bản thiết kế:",
        "装载单位" to "Nạp đơn vị",
        "装载建筑" to "Nạp khối công trình",
        "装载自己" to "Nạp bản thân",
        "观" to "O",
        "观察者模式" to "Chế độ quan sát",
        "规则：" to "Luật:",
        "视角脱离玩家" to "Camera tách khỏi người chơi",
        "解禁" to "Mở khóa",
        "警告：该页功能主要供单机作图使用" to "Cảnh báo: trang này chủ yếu dành cho tạo bản đồ đơn",
        "设定查询波次" to "Đặt đợt mục tiêu",
        "设置目标" to "Đặt mục tiêu",
        "该波次没有敌人" to "Đợt này không có kẻ địch",
        "请不要频繁标记!" to "Đừng đánh dấu quá thường xuyên!",
        "读取回放失败!" to "Đọc bản phát lại thất bại!",
        "超出限制的聊天记录将在载入地图时清除" to "Lịch sử vượt giới hạn sẽ bị xóa khi tải bản đồ",
        "路径" to "Đường dẫn",
        "进入传送带" to "Vào băng chuyền khối hàng",
        "选择代码" to "Chọn mã",
        "选择图片[white](png)" to "Chọn ảnh[white](png)",
        "选择并导入图片，可将其转成画板、像素画或是逻辑画" to "Chọn và nhập ảnh để chuyển thành bảng vẽ, pixel art hoặc tranh logic.",
        "选择范围" to "Phạm vi chọn",
        "逻辑~公告" to "Logic~Thông báo",
        "逻辑~通报" to "Logic~Báo cáo",
        "逻辑画网站" to "Trang web tranh logic",
        "逻辑辅助器[gold]X[]" to "Trợ lý logic[gold]X[]",
        "重建AI" to "AI xây lại",
        "重置" to "Đặt lại",
        "重置所有链接" to "Đặt lại mọi liên kết",
        "锁定" to "Khóa",
        "锁定上个标记点" to "Khóa điểm đánh dấu trước",
        "队伍ID:" to "ID đội:",
        "队伍区域" to "Phạm vi đội",
        "队伍选择器" to "Bộ chọn đội",
        "队伍：" to "Đội:",
        "隐藏全部建筑" to "Ẩn mọi khối",
        "隐藏逻辑辅助器" to "Ẩn trợ lý logic",
        "集合" to "Tập kết",
        "雷达开关" to "Bật/tắt radar",
        "雾" to "F",
        "预览版(更新更快,新功能体验,BUG修复)" to "Bản xem trước\n(cập nhật nhanh, tính năng mới, sửa lỗi)",
        "颜色" to "Màu",
        "飞行模式" to "Chế độ bay"
    )
}
