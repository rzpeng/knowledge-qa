#!/usr/bin/env python3
# -*- coding: utf-8 -*-

from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import cm
from reportlab.lib.colors import HexColor
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak
from reportlab.lib.enums import TA_JUSTIFY, TA_LEFT, TA_CENTER
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
import os

# 注册中文字体
font_path = "/c/Users/rzpme/AppData/Roaming/qianfan-desktop-app/qianfan_desk_xdg/global/data/skills/pdf/scripts/fonts/fangsong/ZhuqueFangsong-Regular.ttf"
if os.path.exists(font_path):
    pdfmetrics.registerFont(TTFont('FangSong', font_path))
    CHINESE_FONT = 'FangSong'
else:
    CHINESE_FONT = 'Helvetica'

# 创建文档
doc = SimpleDocTemplate(
    "影刀与AI工具转行可行性分析.pdf",
    pagesize=A4,
    rightMargin=2*cm,
    leftMargin=2*cm,
    topMargin=2*cm,
    bottomMargin=2*cm
)

# 定义样式
styles = getSampleStyleSheet()

# 标题样式
title_style = ParagraphStyle(
    'CustomTitle',
    parent=styles['Heading1'],
    fontName=CHINESE_FONT,
    fontSize=18,
    textColor=HexColor('#2D5F8A'),
    spaceAfter=12,
    spaceBefore=12,
    alignment=TA_LEFT
)

# 二级标题
h2_style = ParagraphStyle(
    'CustomH2',
    parent=styles['Heading2'],
    fontName=CHINESE_FONT,
    fontSize=14,
    textColor=HexColor('#2D5F8A'),
    spaceAfter=8,
    spaceBefore=12,
    alignment=TA_LEFT
)

# 三级标题
h3_style = ParagraphStyle(
    'CustomH3',
    parent=styles['Heading3'],
    fontName=CHINESE_FONT,
    fontSize=12,
    textColor=HexColor('#333333'),
    spaceAfter=6,
    spaceBefore=10,
    alignment=TA_LEFT
)

# 正文样式
body_style = ParagraphStyle(
    'CustomBody',
    parent=styles['Normal'],
    fontName=CHINESE_FONT,
    fontSize=10,
    leading=16,
    alignment=TA_JUSTIFY,
    spaceAfter=6
)

# 列表项样式
bullet_style = ParagraphStyle(
    'CustomBullet',
    parent=styles['Normal'],
    fontName=CHINESE_FONT,
    fontSize=10,
    leading=16,
    leftIndent=20,
    spaceAfter=4
)

# 编号列表样式
numbered_style = ParagraphStyle(
    'CustomNumbered',
    parent=styles['Normal'],
    fontName=CHINESE_FONT,
    fontSize=10,
    leading=16,
    leftIndent=20,
    spaceAfter=4
)

# 构建内容
story = []

# 一、学习影刀与AI工具的价值
story.append(Paragraph("一、学习影刀与AI工具的价值", title_style))

story.append(Paragraph("1.1 影刀（RPA）能力", h2_style))
story.append(Paragraph("• 自动化数据处理、批量操作", bullet_style))
story.append(Paragraph("• 提升工作效率，适合运营、审核类岗位", bullet_style))
story.append(Paragraph("• 郑州有很多电商公司需要RPA人才", bullet_style))

story.append(Paragraph("1.2 AI工具能力", h2_style))
story.append(Paragraph("• AIGC内容生成（文案、图片、视频）", bullet_style))
story.append(Paragraph("• 提升内容生产效率", bullet_style))
story.append(Paragraph("• 符合行业发展趋势", bullet_style))

# 二、六大岗位可行性分析
story.append(Paragraph("二、六大岗位可行性分析", title_style))

# 2.1 短视频剪辑
story.append(Paragraph("2.1 短视频剪辑", h2_style))
story.append(Paragraph("<b>匹配度：★★★★☆</b>", body_style))

story.append(Paragraph("2.1.1 匹配度分析", h3_style))
story.append(Paragraph("• <b>优势</b>：售前经验让她理解用户需求，知道什么内容能打动客户", bullet_style))
story.append(Paragraph("• <b>挑战</b>：需要学习剪辑软件和创意思维", bullet_style))
story.append(Paragraph("• <b>影刀+AI帮助</b>：影刀批量下载素材、自动发布视频；AI配音、AI字幕、AI特效", bullet_style))

story.append(Paragraph("2.1.2 学习路径（6-8周）", h3_style))
story.append(Paragraph("<b>第1-2周：剪辑基础</b> — 剪映/必剪入门，学习剪辑逻辑、转场、配乐，每天练习剪辑1-2个短视频", body_style))
story.append(Paragraph("<b>第3-4周：AI工具应用</b> — AI配音工具、AI字幕生成、AI特效，影刀自动化批量下载、批量发布", body_style))
story.append(Paragraph("<b>第5-6周：平台规则与运营</b> — 抖音、快手、小红书平台规则，爆款视频拆解，账号定位与内容规划", body_style))
story.append(Paragraph("<b>第7-8周：作品集与求职</b> — 完成10-20个作品，运营一个测试账号，投递郑州MCN机构、电商公司", body_style))

story.append(Paragraph("2.1.3 薪资参考（郑州）", h3_style))
table_data = [
    ['阶段', '薪资范围'],
    ['实习', '3-5K'],
    ['初级', '5-8K'],
    ['1-2年经验', '8-12K'],
    ['资深', '12-20K']
]
table = Table(table_data, colWidths=[5*cm, 5*cm])
table.setStyle(TableStyle([
    ('FONTNAME', (0, 0), (-1, -1), CHINESE_FONT),
    ('FONTSIZE', (0, 0), (-1, -1), 9),
    ('BACKGROUND', (0, 0), (-1, 0), HexColor('#2D5F8A')),
    ('TEXTCOLOR', (0, 0), (-1, 0), HexColor('#FFFFFF')),
    ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
    ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
    ('GRID', (0, 0), (-1, -1), 0.5, HexColor('#CCCCCC')),
    ('ROWBACKGROUNDS', (0, 1), (-1, -1), [HexColor('#F5F5F5'), HexColor('#FFFFFF')]),
    ('TOPPADDING', (0, 0), (-1, -1), 8),
    ('BOTTOMPADDING', (0, 0), (-1, -1), 8),
]))
story.append(Spacer(1, 12))
story.append(table)
story.append(Spacer(1, 6))
story.append(Paragraph("短视频剪辑岗位薪资水平", ParagraphStyle('Caption', parent=styles['Normal'], fontName=CHINESE_FONT, fontSize=8, textColor=HexColor('#666666'), alignment=TA_CENTER)))

story.append(Paragraph("2.1.4 可行性评估", h3_style))
story.append(Paragraph("• 学习周期短，1-2个月可上手", bullet_style))
story.append(Paragraph("• 郑州岗位需求大", bullet_style))
story.append(Paragraph("• AI工具大幅降低门槛", bullet_style))
story.append(Paragraph("• 但竞争激烈，需要持续学习", bullet_style))

# 2.2 新媒体运营
story.append(Paragraph("2.2 新媒体运营", h2_style))
story.append(Paragraph("<b>匹配度：★★★★★</b>", body_style))

story.append(Paragraph("2.2.1 匹配度分析", h3_style))
story.append(Paragraph("• <b>优势</b>：售前经验直接转化为用户洞察、内容策划能力", bullet_style))
story.append(Paragraph("• <b>挑战</b>：需要学习平台规则、数据分析", bullet_style))
story.append(Paragraph("• <b>影刀+AI帮助</b>：影刀自动发布、数据采集、竞品监控；AI写文案、AI生成图片、AI数据分析", bullet_style))

story.append(Paragraph("2.2.2 学习路径（6-8周）", h3_style))
story.append(Paragraph("<b>第1-2周：平台基础</b> — 抖音、小红书、公众号平台规则，内容定位与用户画像，爆款内容拆解", body_style))
story.append(Paragraph("<b>第3-4周：AI工具应用</b> — AI文案生成（ChatGPT、文心一言），AI图片生成（Midjourney、文心一格），影刀自动化定时发布、数据采集", body_style))
story.append(Paragraph("<b>第5-6周：数据分析与优化</b> — 数据指标（阅读量、互动率、转化率），数据分析工具（新榜、蝉妈妈），A/B测试与内容优化", body_style))
story.append(Paragraph("<b>第7-8周：实战与求职</b> — 运营一个测试账号（涨粉目标1000+），准备运营案例，投递郑州互联网公司、电商公司", body_style))

story.append(Paragraph("2.2.3 薪资参考（郑州）", h3_style))
table_data = [
    ['阶段', '薪资范围'],
    ['实习', '3-5K'],
    ['初级', '5-8K'],
    ['1-2年经验', '8-12K'],
    ['资深', '12-18K']
]
table = Table(table_data, colWidths=[5*cm, 5*cm])
table.setStyle(TableStyle([
    ('FONTNAME', (0, 0), (-1, -1), CHINESE_FONT),
    ('FONTSIZE', (0, 0), (-1, -1), 9),
    ('BACKGROUND', (0, 0), (-1, 0), HexColor('#2D5F8A')),
    ('TEXTCOLOR', (0, 0), (-1, 0), HexColor('#FFFFFF')),
    ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
    ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
    ('GRID', (0, 0), (-1, -1), 0.5, HexColor('#CCCCCC')),
    ('ROWBACKGROUNDS', (0, 1), (-1, -1), [HexColor('#F5F5F5'), HexColor('#FFFFFF')]),
    ('TOPPADDING', (0, 0), (-1, -1), 8),
    ('BOTTOMPADDING', (0, 0), (-1, -1), 8),
]))
story.append(Spacer(1, 12))
story.append(table)
story.append(Spacer(1, 6))
story.append(Paragraph("新媒体运营岗位薪资水平", ParagraphStyle('Caption', parent=styles['Normal'], fontName=CHINESE_FONT, fontSize=8, textColor=HexColor('#666666'), alignment=TA_CENTER)))

story.append(Paragraph("2.2.4 可行性评估", h3_style))
story.append(Paragraph("• 售前经验高度匹配", bullet_style))
story.append(Paragraph("• 学习周期短", bullet_style))
story.append(Paragraph("• 郑州岗位需求大", bullet_style))
story.append(Paragraph("• AI工具提升效率", bullet_style))
story.append(Paragraph("• 长远发展好", bullet_style))

# 2.3 内容审核
story.append(Paragraph("2.3 内容审核", h2_style))
story.append(Paragraph("<b>匹配度：★★★☆☆</b>", body_style))

story.append(Paragraph("2.3.1 匹配度分析", h3_style))
story.append(Paragraph("• <b>优势</b>：细心、责任心强，适合审核类工作", bullet_style))
story.append(Paragraph("• <b>挑战</b>：工作内容单一，职业天花板较低", bullet_style))
story.append(Paragraph("• <b>影刀+AI帮助</b>：影刀辅助审核流程自动化，AI辅助审核（但核心还是人工判断）", bullet_style))

story.append(Paragraph("2.3.2 学习路径（2-4周）", h3_style))
story.append(Paragraph("<b>第1周：审核标准</b> — 学习内容审核标准（涉政、涉黄、涉暴等），了解平台规则", body_style))
story.append(Paragraph("<b>第2周：工具使用</b> — 审核后台操作，AI辅助审核工具", body_style))
story.append(Paragraph("<b>第3-4周：求职</b> — 投递字节跳动、快手等大厂郑州审核岗，或本地互联网公司", body_style))

story.append(Paragraph("2.3.3 薪资参考（郑州）", h3_style))
table_data = [
    ['阶段', '薪资范围'],
    ['初级', '4-6K'],
    ['1-2年经验', '6-8K'],
    ['资深', '8-12K（需转管理或专项）']
]
table = Table(table_data, colWidths=[5*cm, 5*cm])
table.setStyle(TableStyle([
    ('FONTNAME', (0, 0), (-1, -1), CHINESE_FONT),
    ('FONTSIZE', (0, 0), (-1, -1), 9),
    ('BACKGROUND', (0, 0), (-1, 0), HexColor('#2D5F8A')),
    ('TEXTCOLOR', (0, 0), (-1, 0), HexColor('#FFFFFF')),
    ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
    ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
    ('GRID', (0, 0), (-1, -1), 0.5, HexColor('#CCCCCC')),
    ('ROWBACKGROUNDS', (0, 1), (-1, -1), [HexColor('#F5F5F5'), HexColor('#FFFFFF')]),
    ('TOPPADDING', (0, 0), (-1, -1), 8),
    ('BOTTOMPADDING', (0, 0), (-1, -1), 8),
]))
story.append(Spacer(1, 12))
story.append(table)
story.append(Spacer(1, 6))
story.append(Paragraph("内容审核岗位薪资水平", ParagraphStyle('Caption', parent=styles['Normal'], fontName=CHINESE_FONT, fontSize=8, textColor=HexColor('#666666'), alignment=TA_CENTER)))

story.append(Paragraph("2.3.4 可行性评估", h3_style))
story.append(Paragraph("• 入门门槛低，上手快", bullet_style))
story.append(Paragraph("• 但职业天花板低", bullet_style))
story.append(Paragraph("• 工作内容枯燥", bullet_style))
story.append(Paragraph("• 不建议作为长期方向", bullet_style))

# 2.4 AI训练师
story.append(Paragraph("2.4 AI训练师", h2_style))
story.append(Paragraph("<b>匹配度：★★★★☆</b>", body_style))

story.append(Paragraph("2.4.1 匹配度分析", h3_style))
story.append(Paragraph("• <b>优势</b>：技术支持背景，理解软件产品逻辑", bullet_style))
story.append(Paragraph("• <b>挑战</b>：需要学习AI基础知识、数据标注", bullet_style))
story.append(Paragraph("• <b>影刀+AI帮助</b>：影刀自动化数据处理、批量标注；理解AI模型原理", bullet_style))

story.append(Paragraph("2.4.2 学习路径（8-10周）", h3_style))
story.append(Paragraph("<b>第1-2周：AI基础</b> — 机器学习、深度学习基础概念，AI模型训练流程，在线课程（吴恩达、李宏毅）", body_style))
story.append(Paragraph("<b>第3-4周：数据标注</b> — 数据标注工具使用，标注规范与质量控制，影刀自动化标注流程", body_style))
story.append(Paragraph("<b>第5-6周：AI工具应用</b> — 大模型应用（ChatGPT、文心一言），Prompt Engineering，AI模型调优", body_style))
story.append(Paragraph("<b>第7-8周：行业应用</b> — 了解AI在垂直行业的应用，选择一个方向深入（电商、教育、医疗等）", body_style))
story.append(Paragraph("<b>第9-10周：求职</b> — 准备项目案例，投递AI公司、互联网公司AI部门", body_style))

story.append(Paragraph("2.4.3 薪资参考（郑州）", h3_style))
table_data = [
    ['阶段', '薪资范围'],
    ['初级', '6-10K'],
    ['1-2年经验', '10-15K'],
    ['资深', '15-25K']
]
table = Table(table_data, colWidths=[5*cm, 5*cm])
table.setStyle(TableStyle([
    ('FONTNAME', (0, 0), (-1, -1), CHINESE_FONT),
    ('FONTSIZE', (0, 0), (-1, -1), 9),
    ('BACKGROUND', (0, 0), (-1, 0), HexColor('#2D5F8A')),
    ('TEXTCOLOR', (0, 0), (-1, 0), HexColor('#FFFFFF')),
    ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
    ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
    ('GRID', (0, 0), (-1, -1), 0.5, HexColor('#CCCCCC')),
    ('ROWBACKGROUNDS', (0, 1), (-1, -1), [HexColor('#F5F5F5'), HexColor('#FFFFFF')]),
    ('TOPPADDING', (0, 0), (-1, -1), 8),
    ('BOTTOMPADDING', (0, 0), (-1, -1), 8),
]))
story.append(Spacer(1, 12))
story.append(table)
story.append(Spacer(1, 6))
story.append(Paragraph("AI训练师岗位薪资水平", ParagraphStyle('Caption', parent=styles['Normal'], fontName=CHINESE_FONT, fontSize=8, textColor=HexColor('#666666'), alignment=TA_CENTER)))

story.append(Paragraph("2.4.4 可行性评估", h3_style))
story.append(Paragraph("• 新兴职业，前景好", bullet_style))
story.append(Paragraph("• 郑州AI产业在发展", bullet_style))
story.append(Paragraph("• 学习周期较长", bullet_style))
story.append(Paragraph("• 需要持续学习", bullet_style))

# 2.5 影视AI漫剧
story.append(Paragraph("2.5 影视AI漫剧", h2_style))
story.append(Paragraph("<b>匹配度：★★★☆☆</b>", body_style))

story.append(Paragraph("2.5.1 匹配度分析", h3_style))
story.append(Paragraph("• <b>优势</b>：创意能力、故事理解能力", bullet_style))
story.append(Paragraph("• <b>挑战</b>：需要美术、编剧能力，岗位较少", bullet_style))
story.append(Paragraph("• <b>影刀+AI帮助</b>：影刀批量处理素材；AI生成漫画、AI配音、AI动画", bullet_style))

story.append(Paragraph("2.5.2 学习路径（8-12周）", h3_style))
story.append(Paragraph("<b>第1-3周：AI绘画工具</b> — Midjourney、Stable Diffusion，AI漫画生成工具，风格控制与一致性", body_style))
story.append(Paragraph("<b>第4-6周：AI视频工具</b> — Runway、Pika等AI视频生成，AI配音、AI字幕，剪辑与后期", body_style))
story.append(Paragraph("<b>第7-9周：内容创作</b> — 故事脚本编写，角色设计与场景设计，完整作品制作", body_style))
story.append(Paragraph("<b>第10-12周：平台与求职</b> — 发布作品到B站、抖音，投递影视公司、MCN机构", body_style))

story.append(Paragraph("2.5.3 薪资参考（郑州）", h3_style))
table_data = [
    ['阶段', '薪资范围'],
    ['初级', '5-8K'],
    ['1-2年经验', '8-15K'],
    ['资深', '15-25K']
]
table = Table(table_data, colWidths=[5*cm, 5*cm])
table.setStyle(TableStyle([
    ('FONTNAME', (0, 0), (-1, -1), CHINESE_FONT),
    ('FONTSIZE', (0, 0), (-1, -1), 9),
    ('BACKGROUND', (0, 0), (-1, 0), HexColor('#2D5F8A')),
    ('TEXTCOLOR', (0, 0), (-1, 0), HexColor('#FFFFFF')),
    ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
    ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
    ('GRID', (0, 0), (-1, -1), 0.5, HexColor('#CCCCCC')),
    ('ROWBACKGROUNDS', (0, 1), (-1, -1), [HexColor('#F5F5F5'), HexColor('#FFFFFF')]),
    ('TOPPADDING', (0, 0), (-1, -1), 8),
    ('BOTTOMPADDING', (0, 0), (-1, -1), 8),
]))
story.append(Spacer(1, 12))
story.append(table)
story.append(Spacer(1, 6))
story.append(Paragraph("影视AI漫剧岗位薪资水平", ParagraphStyle('Caption', parent=styles['Normal'], fontName=CHINESE_FONT, fontSize=8, textColor=HexColor('#666666'), alignment=TA_CENTER)))

story.append(Paragraph("2.5.4 可行性评估", h3_style))
story.append(Paragraph("• 非常新颖的方向", bullet_style))
story.append(Paragraph("• 郑州岗位较少", bullet_style))
story.append(Paragraph("• 需要创意和美术能力", bullet_style))
story.append(Paragraph("• 更适合一线城市", bullet_style))

# 2.6 电商相关岗位
story.append(Paragraph("2.6 电商相关岗位", h2_style))
story.append(Paragraph("<b>匹配度：★★★★★</b>", body_style))

story.append(Paragraph("2.6.1 匹配度分析", h3_style))
story.append(Paragraph("• <b>优势</b>：售前经验转化为客户沟通能力，理解B端需求", bullet_style))
story.append(Paragraph("• <b>挑战</b>：需要学习电商运营、数据分析", bullet_style))
story.append(Paragraph("• <b>影刀+AI帮助</b>：影刀自动上架、数据采集、客服自动化；AI客服、AI文案、AI数据分析", bullet_style))

story.append(Paragraph("2.6.2 电商运营学习路径（6-8周）", h3_style))
story.append(Paragraph("<b>第1-2周：平台规则</b> — 淘宝、拼多多、抖音电商规则，店铺运营基础", body_style))
story.append(Paragraph("<b>第3-4周：AI工具应用</b> — AI写商品文案，AI生成商品图片，影刀自动化批量上架、数据采集", body_style))
story.append(Paragraph("<b>第5-6周：数据分析</b> — 数据指标（流量、转化率、客单价），数据分析工具，竞品分析", body_style))
story.append(Paragraph("<b>第7-8周：实战与求职</b> — 开一个测试店铺，准备运营案例，投递郑州电商公司", body_style))

story.append(Paragraph("2.6.3 电商直播运营学习路径（6-8周）", h3_style))
story.append(Paragraph("<b>第1-2周：直播基础</b> — 直播平台规则（抖音、快手），直播流程与话术", body_style))
story.append(Paragraph("<b>第3-4周：AI工具应用</b> — AI生成直播脚本，AI数据分析，影刀自动化数据采集、竞品监控", body_style))
story.append(Paragraph("<b>第5-6周：数据分析</b> — 直播数据指标，投流策略", body_style))
story.append(Paragraph("<b>第7-8周：实战与求职</b> — 参与几场直播实操，投递郑州直播电商公司", body_style))

story.append(Paragraph("2.6.4 薪资参考（郑州）", h3_style))
table_data = [
    ['岗位', '初级', '1-2年经验', '资深'],
    ['电商运营', '5-8K', '8-12K', '12-18K'],
    ['直播运营', '6-10K', '10-15K', '15-25K'],
    ['电商客服主管', '6-8K', '8-10K', '10-15K']
]
table = Table(table_data, colWidths=[3.5*cm, 2.5*cm, 2.5*cm, 2.5*cm])
table.setStyle(TableStyle([
    ('FONTNAME', (0, 0), (-1, -1), CHINESE_FONT),
    ('FONTSIZE', (0, 0), (-1, -1), 9),
    ('BACKGROUND', (0, 0), (-1, 0), HexColor('#2D5F8A')),
    ('TEXTCOLOR', (0, 0), (-1, 0), HexColor('#FFFFFF')),
    ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
    ('VALIGN', (0, 0), (-1, -1), 'MIDDLE'),
    ('GRID', (0, 0), (-1, -1), 0.5, HexColor('#CCCCCC')),
    ('ROWBACKGROUNDS', (0, 1), (-1, -1), [HexColor('#F5F5F5'), HexColor('#FFFFFF')]),
    ('TOPPADDING', (0, 0), (-1, -1), 8),
    ('BOTTOMPADDING', (0, 0), (-1, -1), 8),
]))
story.append(Spacer(1, 12))
story.append(table)
story.append(Spacer(1, 6))
story.append(Paragraph("电商相关岗位薪资水平", ParagraphStyle('Caption', parent=styles['Normal'], fontName=CHINESE_FONT, fontSize=8, textColor=HexColor('#666666'), alignment=TA_CENTER)))

story.append(Paragraph("2.6.5 可行性评估", h3_style))
story.append(Paragraph("• 郑州电商产业发达", bullet_style))
story.append(Paragraph("• 岗位需求大", bullet_style))
story.append(Paragraph("• 影刀+AI大幅提升效率", bullet_style))
story.append(Paragraph("• 薪资有竞争力", bullet_style))
story.append(Paragraph("• 长远发展好", bullet_style))

# 三、综合排名与建议
story.append(Paragraph("三、综合排名与建议", title_style))

story.append(Paragraph("3.1 第一梯队（强烈推荐）", h2_style))

story.append(Paragraph("3.1.1 新媒体运营", h3_style))
story.append(Paragraph("• 售前经验高度匹配", bullet_style))
story.append(Paragraph("• 学习周期短（6-8周）", bullet_style))
story.append(Paragraph("• 郑州岗位多", bullet_style))
story.append(Paragraph("• AI工具提升效率", bullet_style))
story.append(Paragraph("• 长远发展好", bullet_style))

story.append(Paragraph("3.1.2 电商运营/直播运营", h3_style))
story.append(Paragraph("• 郑州电商产业发达", bullet_style))
story.append(Paragraph("• 影刀+AI应用广泛", bullet_style))
story.append(Paragraph("• 薪资有竞争力", bullet_style))
story.append(Paragraph("• 转行门槛低", bullet_style))

story.append(Paragraph("3.2 第二梯队（推荐）", h2_style))

story.append(Paragraph("3.2.1 短视频剪辑", h3_style))
story.append(Paragraph("• 学习周期短", bullet_style))
story.append(Paragraph("• AI工具降低门槛", bullet_style))
story.append(Paragraph("• 郑州MCN机构多", bullet_style))
story.append(Paragraph("• 但竞争激烈", bullet_style))

story.append(Paragraph("3.2.2 AI训练师", h3_style))
story.append(Paragraph("• 新兴职业，前景好", bullet_style))
story.append(Paragraph("• 技术支持背景有帮助", bullet_style))
story.append(Paragraph("• 学习周期较长", bullet_style))
story.append(Paragraph("• 郑州AI产业在发展", bullet_style))

story.append(Paragraph("3.3 第三梯队（谨慎选择）", h2_style))

story.append(Paragraph("3.3.1 影视AI漫剧", h3_style))
story.append(Paragraph("• 非常新颖", bullet_style))
story.append(Paragraph("• 郑州岗位少", bullet_style))
story.append(Paragraph("• 需要创意能力", bullet_style))
story.append(Paragraph("• 更适合一线城市", bullet_style))

story.append(Paragraph("3.3.2 内容审核", h3_style))
story.append(Paragraph("• 入门快", bullet_style))
story.append(Paragraph("• 但职业天花板低", bullet_style))
story.append(Paragraph("• 工作枯燥", bullet_style))
story.append(Paragraph("• 不建议长期发展", bullet_style))

# 四、最佳组合方案
story.append(Paragraph("四、最佳组合方案", title_style))

story.append(Paragraph("4.1 方案一：新媒体运营 + 短视频剪辑", h2_style))

story.append(Paragraph("4.1.1 学习路径（8周）", h3_style))
story.append(Paragraph("<b>第1-2周</b>：平台基础 + 剪辑入门", body_style))
story.append(Paragraph("<b>第3-4周</b>：AI工具应用（文案、图片、视频）", body_style))
story.append(Paragraph("<b>第5-6周</b>：影刀自动化 + 数据分析", body_style))
story.append(Paragraph("<b>第7-8周</b>：实战项目 + 求职", body_style))

story.append(Paragraph("4.1.2 优势", h3_style))
story.append(Paragraph("• 技能互补", bullet_style))
story.append(Paragraph("• 岗位选择多", bullet_style))
story.append(Paragraph("• 郑州需求大", bullet_style))
story.append(Paragraph("• 薪资8-15K", bullet_style))

story.append(Paragraph("4.2 方案二：电商运营 + AI工具", h2_style))

story.append(Paragraph("4.2.1 学习路径（8周）", h3_style))
story.append(Paragraph("<b>第1-2周</b>：电商平台规则", body_style))
story.append(Paragraph("<b>第3-4周</b>：AI文案 + AI图片 + 影刀自动化", body_style))
story.append(Paragraph("<b>第5-6周</b>：数据分析 + 竞品分析", body_style))
story.append(Paragraph("<b>第7-8周</b>：实战店铺 + 求职", body_style))

story.append(Paragraph("4.2.2 优势", h3_style))
story.append(Paragraph("• 影刀应用广泛", bullet_style))
story.append(Paragraph("• 郑州电商公司多", bullet_style))
story.append(Paragraph("• 薪资有竞争力", bullet_style))
story.append(Paragraph("• 长远发展好", bullet_style))

# 五、学习资源推荐
story.append(Paragraph("五、学习资源推荐", title_style))

story.append(Paragraph("5.1 影刀学习", h2_style))
story.append(Paragraph("• 影刀官方学院（免费课程）", bullet_style))
story.append(Paragraph("• B站影刀教程", bullet_style))
story.append(Paragraph("• 影刀社区案例", bullet_style))

story.append(Paragraph("5.2 AI工具学习", h2_style))
story.append(Paragraph("• ChatGPT/文心一言：文案生成", bullet_style))
story.append(Paragraph("• Midjourney/文心一格：图片生成", bullet_style))
story.append(Paragraph("• 剪映/必剪：AI视频剪辑", bullet_style))
story.append(Paragraph("• Runway/Pika：AI视频生成", bullet_style))

story.append(Paragraph("5.3 平台学习", h2_style))
story.append(Paragraph("• 抖音创作者学院", bullet_style))
story.append(Paragraph("• 小红书创作者中心", bullet_style))
story.append(Paragraph("• 淘宝大学", bullet_style))

# 六、下一步行动建议
story.append(Paragraph("六、下一步行动建议", title_style))

story.append(Paragraph("6.1 如果选择新媒体运营+短视频剪辑", h2_style))
story.append(Paragraph("1. 第1周：学习剪映基础，注册抖音、小红书账号", numbered_style))
story.append(Paragraph("2. 第2周：学习AI文案工具，开始发布内容", numbered_style))
story.append(Paragraph("3. 第3周：学习影刀自动化，提升效率", numbered_style))
story.append(Paragraph("4. 第4周：数据分析，优化内容", numbered_style))

story.append(Paragraph("6.2 如果选择电商运营", h2_style))
story.append(Paragraph("1. 第1周：学习淘宝/拼多多规则", numbered_style))
story.append(Paragraph("2. 第2周：学习AI文案、AI图片工具", numbered_style))
story.append(Paragraph("3. 第3周：学习影刀自动化上架、数据采集", numbered_style))
story.append(Paragraph("4. 第4周：开一个测试店铺", numbered_style))

# 页脚
story.append(Spacer(1, 30))
story.append(Paragraph("文档生成日期：2026年5月", ParagraphStyle('Footer', parent=styles['Normal'], fontName=CHINESE_FONT, fontSize=8, textColor=HexColor('#999999'), alignment=TA_CENTER)))

# 生成PDF
doc.build(story)
print("PDF生成成功：影刀与AI工具转行可行性分析.pdf")
