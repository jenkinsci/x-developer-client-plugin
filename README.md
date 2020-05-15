# X-Developer Client Jenkins Plugin

- [English](https://github.com/FieldTech/x-developer-client-plugin/blob/master/README.md)
- [中文](https://github.com/FieldTech/x-developer-client-plugin/blob/master/README-zh-cn.md)

This plugin connects GitHub and X-Developer service - Git Analytics Platform for Engineering Productivity.

## Usage

This plugin authenticate with X-Developer server by Jenkins global configuration `APPID` and `APPKEY`, it send `git log` csv file to X-Developer team analysis service with `TEAMID` whenever Jenkins build is triggered.

### 1. Create X-Developer account

Enter [X-Developer Site](https://x-developer.cn) and register an account for free.

Get your `APPID` `APPKEY` from [API](https://x-developer.cn/accounts/api) page.

### 2. Create team

- For free users, you could only create public analysis project, **it means everyone could access your team reports,** we recommend this to **opensource project teams.**
- Private project is only for paid users.

Once you created a project, check your `TEAMID` from [API](https://x-developer.cn/accounts/api) page.

### 3. Config X-Developer Jenkins Plugin

#### Configure global settings

After installed this plugin, enter Manage Jenkins -> Configure System, specify `APPID` `APPKEY` in X-Developer panel.

#### Configure project settings

In your Jenkins project setting, add a post build step, select X-Developer Analysis in dropdown list, and specify `TEAMID`.

- Check `Master` if this job is monitoring master branch of repository, otherwise, keep it uncheck status.
- Check `Force` if you want to trigger analysis service in each build job, otherwise, keep it uncheck status.

### 4. Build and Analysis

The plugin will work with Jenkins build, and print the result of analysis service within build log.

### 5. X-Developer Reports

X-Developer will send you email when analysis job is completed, you could open report through the link in it.

---

## Demo

Feel free to access these [Demo Projects](https://x-developer.cn/projects/).

## Support

Any question or request, please contact us via [support@withfield.tech](mailto:support@withfield.tech)

## License

All the scripts and documentation in this project under the [MIT License](https://github.com/FieldTech/x-developer-analysis-actions/blob/master/LICENSE).