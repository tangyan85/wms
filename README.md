# WMS
基于SSM框架的仓库管理系统



## 功能

* 系统操作权限管理。系统提供基本的登入登出功能，同时系统包含两个角色：系统超级管理员和普通管理员，超级管理员具有最高的操作权限，而普通管理员仅具有最基本的操作权限，而且仅能操作自己被指派的仓库。
* 请求URL鉴权。对于系统使用者登陆后进行操作发送请求的URL，后台会根据当前用户的角色判断是否拥有请求该URL的权限。
* 基础数据信息管理。对包括：货物信息、供应商信息、客户信息、仓库信息在内的基础数据信息进行管理，提供的操作有：添加、删除、修改、条件查询、导出为Excel和到从Excel导入。
* 仓库管理员管理。对仓库管理员信息CRUD操作，或者为指定的仓库管理员指派所管理的仓库。上述中的仓库管理员可以以普通管理员身份登陆到系统。
* 库存信息管理。对库存信息的CRUD操作，导入导出操作，同时查询的时候可以根据仓库以及商品ID等信息进行多条件查询。
* 基本仓库事务操作。执行货物的入库与出库操作。
* 系统登陆日志查询。超级管理员可以查询某一用户在特定时间段内的系统登陆日志。
* 系统操作日志查询。超级管理员可以查询某一用户在特定时间段内对系统进行操作的操作记录。、
* 密码修改。



## 使用到的框架和库

* Apache POI
* MyBatis
* Spring Framework
* Spring MVC
* Apache Shiro
* Ehcache
* Apache Commons
* Log4j
* Slf4j
* Jackson
* C3P0
* Junit
* MySQL-Connector
* jQuery
* Bootstrap



## 部分截图

![](https://raw.githubusercontent.com/KEN-LJQ/MarkdownPics/master/Resource/2017-3-9/WMS-%E6%88%AA%E5%9B%BE1.PNG)

![](https://raw.githubusercontent.com/KEN-LJQ/MarkdownPics/master/Resource/2017-3-9/MWS-%E6%88%AA%E5%9B%BE2.PNG)

![](https://raw.githubusercontent.com/KEN-LJQ/MarkdownPics/master/Resource/2017-3-9/WMS-%E6%88%AA%E5%9B%BE3.PNG)

![](https://raw.githubusercontent.com/KEN-LJQ/MarkdownPics/master/Resource/2017-3-9/WMS-%E6%88%AA%E5%9B%BE4.PNG)

![](https://raw.githubusercontent.com/KEN-LJQ/MarkdownPics/master/Resource/2017-3-9/WMS-%E6%88%AA%E5%9B%BE5.PNG)

![](https://raw.githubusercontent.com/KEN-LJQ/MarkdownPics/master/Resource/2017-3-9/WMS-%E6%88%AA%E5%9B%BE7.PNG)

![](https://raw.githubusercontent.com/KEN-LJQ/MarkdownPics/master/Resource/2017-3-9/WMS-%E6%88%AA%E5%9B%BE8.PNG)

![](https://raw.githubusercontent.com/KEN-LJQ/MarkdownPics/master/Resource/2017-3-9/WMS-%E6%88%AA%E5%9B%BE8.PNG)

![](https://raw.githubusercontent.com/KEN-LJQ/MarkdownPics/master/Resource/2017-3-9/WMS-%E6%88%AA%E5%9B%BE9.PNG)