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
    "职业规划可行性方案.pdf",
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

# 一、核心优势分析
story.append(Paragraph("一、核心优势分析", title_style))
story.append(Paragraph("基于30岁女性转行者的背景和需求，以下是可迁移的核心技能：", body_style))
story.append(Paragraph("• <b>客户洞察</b>：面向设计院、研究院、高校的售前经验，理解B端客户需求", bullet_style))
story.append(Paragraph("• <b>技术理解力</b>：桥梁软件技术支持背景，具备软件产品思维", bullet_style))
story.append(Paragraph("• <b>沟通表达</b>：方案演示、技术讲解经验，适合需要跨部门协作的岗位", bullet_style))
story.append(Paragraph("• <b>行业认知</b>：了解专业软件的交付流程和客户痛点", bullet_style))

# 二、推荐方向
story.append(Paragraph("二、推荐方向（按优先级排序）", title_style))

# 2.1 方向一
story.append(Paragraph("2.1 方向一：产品经理/产品助理（强烈推荐）", h2_style))
story.append(Paragraph("<b>匹配度：★★★★★</b>", body_style))

story.append(Paragraph("2.1.1 为什么适合", h3_style))
story.append(Paragraph("• 售前经验直接转化为需求分析能力", bullet_style))
story.append(Paragraph("• 技术支持背景让她理解产品落地问题", bullet_style))
story.append(Paragraph("• 1-2个月可以掌握基础方法论并找到实习", bullet_style))
story.append(Paragraph("• 郑州互联网公司有产品岗位需求", bullet_style))
story.append(Paragraph("• 薪资在郑州可达8-15K（中级），长远可到20K+", bullet_style))

story.append(Paragraph("2.1.2 学习路径（8周）", h3_style))
story.append(Paragraph("<b>第1-2周：产品基础</b>", body_style))
story.append(Paragraph("• 学习《人人都是产品经理》《启示录》", bullet_style))
story.append(Paragraph("• 掌握需求分析、用户画像、竞品分析方法", bullet_style))
story.append(Paragraph("• 工具：Axure/Figma原型设计、XMind思维导图", bullet_style))

story.append(Paragraph("<b>第3-4周：实战演练</b>", body_style))
story.append(Paragraph("• 选择1-2个郑州本地产品做竞品分析报告", bullet_style))
story.append(Paragraph("• 练习写PRD（产品需求文档）", bullet_style))
story.append(Paragraph("• 学习SQL基础数据查询（老公可指导）", bullet_style))

story.append(Paragraph("<b>第5-6周：项目作品</b>", body_style))
story.append(Paragraph("• 完成一个完整的产品设计项目（从需求到原型）", bullet_style))
story.append(Paragraph("• 整理过往售前案例，转化为产品思维案例", bullet_style))
story.append(Paragraph("• 准备作品集", bullet_style))

story.append(Paragraph("<b>第7-8周：求职准备</b>", body_style))
story.append(Paragraph("• 优化简历，突出售前经验中的产品思维", bullet_style))
story.append(Paragraph("• 模拟面试，准备需求分析、产品设计类问题", bullet_style))
story.append(Paragraph("• 投递郑州本地互联网公司（UU跑腿、咿啦看书、中钢网等）", bullet_style))

story.append(Paragraph("2.1.3 求职策略", h3_style))
story.append(Paragraph("• 目标公司：郑州本地互联网公司、软件公司、SaaS企业", bullet_style))
story.append(Paragraph("• 岗位关键词：产品助理、产品专员、需求分析师", bullet_style))
story.append(Paragraph("• 薪资预期：实习3-5K，转正后6-10K，1-2年后10-15K", bullet_style))

# 2.2 方向二
story.append(Paragraph("2.2 方向二：前端开发工程师", h2_style))
story.append(Paragraph("<b>匹配度：★★★★☆</b>", body_style))

story.append(Paragraph("2.2.1 为什么适合", h3_style))
story.append(Paragraph("• 老公是Java程序员，可以提供技术指导和代码review", bullet_style))
story.append(Paragraph("• 前端学习曲线相对平缓，1-2个月可以做出作品", bullet_style))
story.append(Paragraph("• 郑州前端岗位需求量大", bullet_style))
story.append(Paragraph("• 薪资在郑州6-12K（初中级），长远可达15-20K", bullet_style))

story.append(Paragraph("2.2.2 学习路径（8周）", h3_style))
story.append(Paragraph("<b>第1-2周：HTML/CSS基础</b>", body_style))
story.append(Paragraph("• HTML5语义化标签", bullet_style))
story.append(Paragraph("• CSS布局（Flex、Grid）", bullet_style))
story.append(Paragraph("• 完成静态页面练习", bullet_style))

story.append(Paragraph("<b>第3-4周：JavaScript核心</b>", body_style))
story.append(Paragraph("• ES6+语法", bullet_style))
story.append(Paragraph("• DOM操作、事件处理", bullet_style))
story.append(Paragraph("• 老公指导代码规范", bullet_style))

story.append(Paragraph("<b>第5-6周：框架入门（Vue.js）</b>", body_style))
story.append(Paragraph("• Vue3基础语法", bullet_style))
story.append(Paragraph("• 组件化开发", bullet_style))
story.append(Paragraph("• 完成一个小项目（待办事项、博客等）", bullet_style))

story.append(Paragraph("<b>第7-8周：项目实战</b>", body_style))
story.append(Paragraph("• 完成2-3个完整项目", bullet_style))
story.append(Paragraph("• 学习Git版本控制", bullet_style))
story.append(Paragraph("• 部署到GitHub Pages展示", bullet_style))

story.append(Paragraph("2.2.3 求职策略", h3_style))
story.append(Paragraph("• 目标公司：互联网公司、软件外包公司、企业IT部门", bullet_style))
story.append(Paragraph("• 岗位关键词：前端实习生、前端开发工程师", bullet_style))
story.append(Paragraph("• 薪资预期：实习2-4K，转正后5-8K，1-2年后8-12K", bullet_style))

# 2.3 方向三
story.append(Paragraph("2.3 方向三：项目管理助理/实施顾问", h2_style))
story.append(Paragraph("<b>匹配度：★★★★☆</b>", body_style))

story.append(Paragraph("2.3.1 为什么适合", h3_style))
story.append(Paragraph("• 售前经验直接转化为项目沟通能力", bullet_style))
story.append(Paragraph("• 技术支持背景适合软件实施岗位", bullet_style))
story.append(Paragraph("• 郑州有很多软件公司需要实施顾问", bullet_style))
story.append(Paragraph("• 薪资稳定，郑州6-12K", bullet_style))

story.append(Paragraph("2.3.2 学习路径（6周）", h3_style))
story.append(Paragraph("<b>第1-2周：项目管理基础</b>", body_style))
story.append(Paragraph("• 学习PMP基础知识", bullet_style))
story.append(Paragraph("• 掌握项目计划、进度管理方法", bullet_style))
story.append(Paragraph("• 工具：Project、禅道、Jira", bullet_style))

story.append(Paragraph("<b>第3-4周：软件实施流程</b>", body_style))
story.append(Paragraph("• 学习软件部署、配置、培训流程", bullet_style))
story.append(Paragraph("• 了解常见企业软件（ERP、CRM、OA）", bullet_style))
story.append(Paragraph("• 练习编写实施文档", bullet_style))

story.append(Paragraph("<b>第5-6周：求职准备</b>", body_style))
story.append(Paragraph("• 整理过往项目案例", bullet_style))
story.append(Paragraph("• 准备面试话术", bullet_style))
story.append(Paragraph("• 投递软件公司实施岗位", bullet_style))

# 三、综合建议
story.append(Paragraph("三、综合建议", title_style))

story.append(Paragraph("3.1 首选：产品经理方向", h2_style))
story.append(Paragraph("<b>理由：</b>", body_style))
story.append(Paragraph("1. <b>技能迁移度高</b>：售前经验是产品经理的核心能力之一", numbered_style))
story.append(Paragraph("2. <b>学习周期短</b>：2个月可以掌握基础并找到实习", numbered_style))
story.append(Paragraph("3. <b>长远发展好</b>：产品经理是核心岗位，职业天花板高", numbered_style))
story.append(Paragraph("4. <b>薪资有竞争力</b>：郑州中等偏上水平", numbered_style))
story.append(Paragraph("5. <b>女性友好</b>：产品岗位注重沟通和协调，适合女性长期发展", numbered_style))

story.append(Paragraph("3.2 备选：前端开发", h2_style))
story.append(Paragraph("<b>适用情况：</b>", body_style))
story.append(Paragraph("• 如果她对编程有兴趣，且老公愿意投入时间指导", bullet_style))
story.append(Paragraph("• 如果她更喜欢技术路线而非管理路线", bullet_style))

# 四、行动计划
story.append(Paragraph("四、行动计划（产品经理方向）", title_style))

story.append(Paragraph("4.1 第1周：快速启动", h2_style))
story.append(Paragraph("• 购买/借阅《人人都是产品经理》《启示录》", bullet_style))
story.append(Paragraph("• 注册产品经理社区（人人都是产品经理、PMCAFF）", bullet_style))
story.append(Paragraph("• 下载Axure/Figma，开始学习基础操作", bullet_style))
story.append(Paragraph("• 列出过往售前案例，提炼需求分析经验", bullet_style))

story.append(Paragraph("4.2 第2-4周：系统学习", h2_style))
story.append(Paragraph("• 每天学习2-3小时（晚上+周末）", bullet_style))
story.append(Paragraph("• 完成1个竞品分析报告", bullet_style))
story.append(Paragraph("• 完成1个PRD文档", bullet_style))
story.append(Paragraph("• 学习SQL基础（老公指导）", bullet_style))

story.append(Paragraph("4.3 第5-6周：作品准备", h2_style))
story.append(Paragraph("• 完成1个完整产品设计项目", bullet_style))
story.append(Paragraph("• 整理作品集（PDF+在线链接）", bullet_style))
story.append(Paragraph("• 准备简历（突出售前经验的产品思维）", bullet_style))

story.append(Paragraph("4.4 第7-8周：求职冲刺", h2_style))
story.append(Paragraph("• 投递郑州本地公司（每天3-5家）", bullet_style))
story.append(Paragraph("• 参加线下产品经理聚会/沙龙", bullet_style))
story.append(Paragraph("• 模拟面试，优化表达", bullet_style))

# 五、郑州目标公司清单
story.append(Paragraph("五、郑州目标公司清单", title_style))

story.append(Paragraph("5.1 互联网公司", h2_style))
story.append(Paragraph("• UU跑腿（物流配送平台）", bullet_style))
story.append(Paragraph("• 咿啦看书（儿童阅读平台）", bullet_style))
story.append(Paragraph("• 中钢网（钢铁电商）", bullet_style))
story.append(Paragraph("• 世界的网（本地生活服务）", bullet_style))

story.append(Paragraph("5.2 软件公司", h2_style))
story.append(Paragraph("• 新开普电子（校园信息化）", bullet_style))
story.append(Paragraph("• 信大捷安（信息安全）", bullet_style))
story.append(Paragraph("• 新华电脑（教育软件）", bullet_style))
story.append(Paragraph("• 各类SaaS软件公司", bullet_style))

story.append(Paragraph("5.3 传统企业IT部门", h2_style))
story.append(Paragraph("• 建业集团", bullet_style))
story.append(Paragraph("• 宇通客车", bullet_style))
story.append(Paragraph("• 郑煤机", bullet_style))
story.append(Paragraph("• 各类制造业企业", bullet_style))

# 六、薪资参考
story.append(Paragraph("六、薪资参考（郑州市场）", title_style))

# 创建表格
table_data = [
    ['岗位', '实习期', '转正后', '1-2年经验', '3-5年经验'],
    ['产品助理', '3-5K', '6-8K', '8-12K', '12-18K'],
    ['前端开发', '2-4K', '5-8K', '8-12K', '12-20K'],
    ['实施顾问', '3-5K', '5-8K', '8-12K', '10-15K']
]

table = Table(table_data, colWidths=[3*cm, 2.5*cm, 2.5*cm, 2.5*cm, 2.5*cm])
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
story.append(Paragraph("郑州市场各岗位薪资水平对比（单位：K/月）", ParagraphStyle(
    'Caption',
    parent=styles['Normal'],
    fontName=CHINESE_FONT,
    fontSize=8,
    textColor=HexColor('#666666'),
    alignment=TA_CENTER
)))

# 七、风险提示
story.append(Paragraph("七、风险提示", title_style))
story.append(Paragraph("1. <b>年龄因素</b>：30岁转行需要更快证明自己，建议选择有经验优势的方向（产品经理）", numbered_style))
story.append(Paragraph("2. <b>地域限制</b>：郑州互联网行业不如一线城市，但生活成本也低，性价比合理", numbered_style))
story.append(Paragraph("3. <b>学习投入</b>：前2个月需要高强度学习，建议全职投入或请假学习", numbered_style))

# 八、下一步行动
story.append(Paragraph("八、下一步行动", title_style))
story.append(Paragraph("建议她先花1周时间了解产品经理工作内容，可以：", body_style))
story.append(Paragraph("1. 看B站产品经理入门视频", numbered_style))
story.append(Paragraph("2. 读《人人都是产品经理》前3章", numbered_style))
story.append(Paragraph("3. 找产品经理朋友聊聊日常工作", numbered_style))
story.append(Spacer(1, 12))
story.append(Paragraph("如果觉得适合，就按上述计划执行；如果觉得不适合，再考虑前端开发或项目管理方向。", body_style))

# 页脚
story.append(Spacer(1, 30))
story.append(Paragraph("文档生成日期：2026年5月", ParagraphStyle(
    'Footer',
    parent=styles['Normal'],
    fontName=CHINESE_FONT,
    fontSize=8,
    textColor=HexColor('#999999'),
    alignment=TA_CENTER
)))

# 生成PDF
doc.build(story)
print("PDF生成成功：职业规划可行性方案.pdf")
