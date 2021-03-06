# X-Developer Client Jenkins Plugin

[![](https://ci.jenkins.io/job/Plugins/job/x-developer-client-plugin/job/master/badge/icon)](https://ci.jenkins.io/job/Plugins/job/x-developer-client-plugin/job/master)
[![EN doc](https://img.shields.io/badge/document-English-blue.svg)](https://github.com/jenkinsci/x-developer-client-plugin/blob/master/README.md)
[![CN doc](https://img.shields.io/badge/文档-中文版-blue.svg)](https://github.com/jenkinsci/x-developer-client-plugin/blob/master/README-zh-cn.md)


> X-Developer Client Plugin 在 Jenkins 构建任务完成后，触发 X-Developer 团队效能分析并通知分析结果。

[X-Developer](https://x-developer.cn) 由场量科技研发，是全球第一款事实数据型研发效能度量分析平台。使用 X-Developer 平台及其开源工具，无需购买、设置或管理任何基础设施和任何开发工作，您只需登录即可开始开展研发团队效能改进。目前，X-Developer 提供了最便捷、完整的研发效能度量解决方案，让您能够以开发者为中心展开改进活动，使您的团队能够围绕目标协同工作，及时同步项目进展，从而将他们从繁重的任务状态维护、项目报告工作中解放出来，集中精力完成研发工作，更好地编写代码，提高业务获得的价值。

包含了产品使用、集成、开源工具以及最佳实践的产品文档已正式发布，[点此查看](https://fieldtech.github.io/xdocs)。

## 如何使用

当 Jenkins 完成构建时，本插件使用 Jenkins 全局配置的 `APPID` 和 `APPKEY`，完成 [X-Developer](https://x-developer.cn) 身份认证，并发送 `git log` csv 文件到 X-Developer 对应 `TEAMID` 的团队，完成效能分析。

您只需要五个步骤，即可完成插件的配置和启用 X-Developer 分析功能。

### 1. 创建 X-Developer 帐户

[免费注册](https://x-developer.cn/accounts/signup/) 一个 X-Developer 帐户，注册完成后，可在 [API](https://x-developer.cn/accounts/api) 页面查看您的 `APPID` `APPKEY` 。

### 2. 创建团队

- 免费用户只能创建公开团队，公开团队将被展示到 [X-Developer 探索](https://x-developer.cn/projects/) 栏目，**但只有团队内的开发者可查看到内部分析报告**，适用于开源项目。
- 私有团队仅对付费用户开放。

创建团队完成后，即可在 [API](https://x-developer.cn/accounts/api) 页面查看对应的 `TEAMID` 。

### 3. 配置 X-Developer Client Jenkins 插件

进入Jenkins管理界面-->管理插件-->高级，上传插件界面中，选择您下载的插件文件，上传后将自动安装，无须重新启动。


#### 全局配置

进入Jenkins管理界面，您将看到 X-Developer Client Plugin 配置项，填写 `APPID` 和 `APPKEY` ，点击 Test connection，如果正确将返回“X-Developer 认证成功”。

#### 构建任务配置

在团队的构建流水线上，添加“构建后操作”，选择 X-Developer 团队效能分析，填写 `TEAMID` ，保存。

- 如果此构建任务是主干分支，请勾选“主干”；如果是开发测试分支，此项请勿勾选。
- 如果您需要每次构建后都执行分析，请勾选“强制分析”。默认情况下，X-Developer 每天 17:00 ~ 20:00 执行分析任务。

### 4. 构建与分析

每次 Jenkins 完成构建任务后，会触发 X-Developer Client 并执行数据传输和分析任务，执行结果将会打印在构建日志中。

### 5. X-Developer 报表

分析完成后，X-Developer 将发送一封通知邮件到您的注册邮件，通过邮件中的链接，您即可查看到分析结果报告。

---

## 演示

访问 [公开团队](https://x-developer.cn/projects/) 查看已有的分析项目，如果您是这些团队中的一员，可以直接点击"我是开发者"申请访问内部的分析报告。

## 文档

X-Developer 产品文档包括了产品使用、集成、开源工具以及最佳实践，[点此查看](https://fieldtech.github.io/xdocs)。

## 支持

如果您有任何问题或需求，请与我们联系 [support@withfield.tech](mailto:support@withfield.tech)

## 许可证

项目中所有的脚本、文档都遵循 [MIT License](https://github.com/FieldTech/x-developer-client-plugin/blob/master/LICENSE)。
