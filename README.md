# 1.项目名称

mt3561平台通用倒车应用

代码仓库：
git@172.168.0.64:Everest-Project/FlyBackcar.git

# 2.分支说明

只有master分支

# 3.功能描述
显示汽车倒车时候的影像

# 4.编译方式

ssh pengchong@172.168.1.20

密码：pengchong

cd mt3561

. build/envsetup.sh

lunch 25

cd frameworks/base/packages/FlyBackcar

mm

# 5.类说明
BackcarService.java 监听摄像头是否初始化好，监听倒车的状态，发起停止倒车和开启倒车

Backcar_GPIO.java 获取系统倒车节点的数据状态

FlyBackcarUI.java 倒车影像的显示

CameraSurfaceView.java 通过surfaceview来显示倒车的影像

# 6.项目进度
完成
