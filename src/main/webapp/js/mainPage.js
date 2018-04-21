$(function() {
	submitPasswordModify();
	menuClickAction();
	welcomePageInit();
	passwordModifyInit();
	signOut();
	homePage();
});

// 加载欢迎界面
function welcomePageInit(){
	$('#panel').load('pagecomponent/welcomePage.jsp');
}

// 跳回首页
function homePage(){
	$('.home').click(function(){
		$('#panel').load('pagecomponent/welcomePage.jsp');
	})
}


// 动作延时
var delay = (function(){
		var timer = 0;
		return function(callback, ms){
		clearTimeout (timer);
		timer = setTimeout(callback, ms);
		};
	})();


// 侧边栏连接点击动作
function menuClickAction() {
	$(".menu_item").click(function() {
		var url = $(this).attr("name");
		$('#panel .panel').mLoading('show');
		delay(function(){
			$('#panel').load(url);
		}, 500);
	})
}

// 注销登陆
function signOut() {
	$("#signOut").click(function() {
		$.ajax({
			type : "GET",
			url : "account/logout",
			dataType : "json",
			contentType : "application/json",
			success:function(response){
				window.location.reload(true);
			},error:function(response){
				
			}
		})
	})
}

// 显示操作结果提示模态框
function showMsg(type, msg, append) {
	$('#info_success').removeClass("hide");
	$('#info_error').removeClass("hide");
	if (type == "success") {
		$('#info_error').addClass("hide");
	} else if (type == "error") {
		$('#info_success').addClass("hide");
	}
	$('#info_summary').text(msg);
	$('#info_content').text(append);
	$('#global_info_modal').modal("show");
}

// 处理 Ajax 错误响应
function handleAjaxError(responseStatus){
	var type = 'error';
	var msg  = '';
	var append = '';
	if (responseStatus == 403) {
		msg = '未授权操作';
		append = '对不起，您未授权执行此操作，请重新登陆';
		showMsg(type, msg, append);
		// 刷新重新登陆
		delay(function(){
			window.location.reload(true);
		}, 5000);
	} else if (responseStatus == 404) {
		msg = '不存在的操作';
		showMsg(type, msg, append);
	} else if (responseStatus == 430){
		msg = '您的账号在其他地方登陆';
		append = '请确认是否为您本人的操作。若否请及时更换密码';
		showMsg(type, msg, append);
		// 刷新重新登陆
		delay(function(){
			window.location.reload(true);
		}, 5000);
	}else if (responseStatus == 500) {
		msg = '服务器错误';
		append = '对不起，服务器发生了错误，我们将尽快解决，请稍候重试';
		showMsg(type, msg, append);
	} else {
		msg = '遇到未知的错误';
		showMsg(type, msg, append);
	};
}

// 初始密码修改
function passwordModifyInit(){
	bootstrapValidatorInit();

	// 是否弹出密码修改模态框
	isPopPasswordModal = $('#isFirstLogin').text();
	if (isPopPasswordModal == 'true') {
		$('#init_password_modify').modal('show');
	}
}

// 输入校验初始化
function bootstrapValidatorInit(){
	$('#form').bootstrapValidator({
		message:'This value is not valid',
		feedbackIcons:{
			valid:'glyphicon glyphicon-ok',
			invalid:'glyphicon glyphicon-remove',
			validating:'glyphicon glyphicon-refresh'
		},
		excluded: [':disabled'],
		fields:{// 字段验证
			oldPassword:{// 原密码
				validators:{
					notEmpty:{
						message:'输入不能为空'
					},
					callback:{}
				}
			},
			newPassword:{// 新密码
				validators:{
					notEmpty:{
						message:'输入不能为空'
					},
					stringLength:{
						min:6,
						max:16,
						message:'密码长度为6~16位'
					},
					callback:{}
				}
			},
			newPassword_re:{// 重复新密码
				validators:{
					notEmpty:{
						message:'输入不能为空'
					},
					identical:{
						field:'newPassword',
						message:'两次密码不一致'
					}
				}
			}
		}
	})
}

// 密码加密模块
function passwordEncrying(userID,password){
	var str1 = $.md5(password);
	//var str2 = $.md5(str1 + userID);
	return str1;
}

// 密码修改提交
function submitPasswordModify(){
	$('#init_password_modify_submit').click(function(event) {
		var userID = $('#userID').html();
		var oldPassword = $('#oldPassword').val();
		var newPassword = $('#newPassword').val();
		var rePassword = $('#newPassword_re').val();

		oldPassword = passwordEncrying(userID, oldPassword);
		newPassword = passwordEncrying(userID, newPassword);
		rePassword = passwordEncrying(userID, rePassword);
		var data = {
				"oldPassword" : oldPassword,
				"newPassword" : newPassword,
				"rePassword" : rePassword
			}

		// 将数据通过 AJAX 发送到后端
		$.ajax({
			type: "POST",
			url:"account/passwordModify",
			dataType:"json",
			contentType:"application/json",
			data:JSON.stringify(data),
			success:function(response){
				// 接收并处理后端返回的响应e'd'
				if(response.result == "error"){
					var errorMessage;
					if(response.msg == "passwordError"){
						errorMessage = "密码错误";
						field = "oldPassword"
					}else if(response.msg == "passwordUnmatched"){
						errorMessage = "密码不一致";
						field = "newPassword"
					}else if(response.msg == "passwordisold"){
						errorMessage = "不能使用原密码";
						field = "newPassword"
					}

					$('form').data('bootstrapValidator').updateMessage(field,'callback',errorMessage);
					$('form').data('bootstrapValidator').updateStatus(field,'INVALID','callback');
				}else{
					// 否则更新成功，弹出模态框并清空表单
					$('#init_password_modify').modal('hide');
					$('#reset').trigger("click");
					$('#form').bootstrapValidator("resetForm",true); 
				}
				
			},
			error:function(xhr, textStatus, errorThrown){
				// handler error
				handleAjaxError(xhr.status);
			}
		});
	});
}