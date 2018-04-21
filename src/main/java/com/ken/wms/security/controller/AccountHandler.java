package com.ken.wms.security.controller;

import com.ken.wms.common.service.Interface.RepositoryAdminManageService;
import com.ken.wms.common.service.Interface.SystemLogService;
import com.ken.wms.common.util.Response;
import com.ken.wms.common.util.ResponseFactory;
import com.ken.wms.domain.RepositoryAdmin;
import com.ken.wms.domain.UserInfoDTO;
import com.ken.wms.exception.RepositoryAdminManageServiceException;
import com.ken.wms.exception.SystemLogServiceException;
import com.ken.wms.exception.UserAccountServiceException;
import com.ken.wms.security.service.Interface.AccountService;
import com.ken.wms.security.util.CaptchaGenerator;
import org.apache.log4j.Logger;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 用户账户请求 Handler
 *
 * @author Ken
 * @since 017/2/26.
 */
@Controller
@RequestMapping("/account")
public class AccountHandler {

    private static Logger log = Logger.getLogger("application");

    @Autowired
    private AccountService accountService;
    @Autowired
    private SystemLogService systemLogService;
    @Autowired
    private RepositoryAdminManageService repositoryAdminManageService;

    private static final String USER_ID = "id";
    private static final String USER_NAME = "userName";
    private static final String USER_PASSWORD = "password";

    /**
     * 登陆账户
     *
     * @param user 账户信息
     * @return 返回一个 Map 对象，其中包含登陆操作的结果
     */
    @SuppressWarnings("unchecked")
    @RequestMapping(value = "login", method = RequestMethod.POST)
    public
    @ResponseBody
    Map<String, Object> login(@RequestBody Map<String, Object> user) {
        // 初始化 Response
        Response response = ResponseFactory.newInstance();
        String result = Response.RESPONSE_RESULT_ERROR;
        String errorMsg = "";

        // 获取当前的用户的 Subject
        Subject currentUser = SecurityUtils.getSubject();

        // 判断用户是否已经登陆
        if (currentUser != null && !currentUser.isAuthenticated()) {
            String id = (String) user.get(USER_ID);
            String password = (String) user.get(USER_PASSWORD);
            Session session = currentUser.getSession();
            UsernamePasswordToken token = new UsernamePasswordToken(id, password);

            try {
                // 执行登陆操作
                currentUser.login(token);

                /* 设置 session 中 userInfo 的其他信息 */
                UserInfoDTO userInfo = (UserInfoDTO) session.getAttribute("userInfo");
                // 设置登陆IP
                userInfo.setAccessIP(session.getHost());
                // 查询并设置用户所属的仓库ID
                List<RepositoryAdmin> repositoryAdmin = (List<RepositoryAdmin>) repositoryAdminManageService.selectByID(userInfo.getUserID()).get("data");
                userInfo.setRepositoryBelong(-1);
                if (!repositoryAdmin.isEmpty()) {
                    Integer repositoryBelong = repositoryAdmin.get(0).getRepositoryBelongID();
                    if (repositoryBelong != null) {
                        userInfo.setRepositoryBelong(repositoryBelong);
                    }
                }

                // 记录登陆日志
                systemLogService.insertAccessRecord(userInfo.getUserID(), userInfo.getUserName(),
                        userInfo.getAccessIP(), SystemLogService.ACCESS_TYPE_LOGIN);

                // 设置登陆成功响应
                result = Response.RESPONSE_RESULT_SUCCESS;

            } catch (UnknownAccountException e) {
                errorMsg = "unknownAccount";
            } catch (IncorrectCredentialsException e) {
                errorMsg = "incorrectCredentials";
            } catch (AuthenticationException e) {
                errorMsg = "authenticationError";
                e.printStackTrace();
            } catch (SystemLogServiceException | RepositoryAdminManageServiceException e) {
                errorMsg = "ServerError";
            } finally {
                // 当登陆失败则清除session中的用户信息
                if (result.equals(Response.RESPONSE_RESULT_ERROR)){
                    session.setAttribute("userInfo", null);
                }
            }
        } else {
            errorMsg = "already login";
        }

        // 设置 Response
        response.setResponseResult(result);
        response.setResponseMsg(errorMsg);
        return response.generateResponse();
    }

    /**
     * 注销账户
     *
     * @return 返回一个 Map 对象，键值为 result 的内容代表注销操作的结果，值为 success 或 error
     */
    @RequestMapping(value = "logout", method = RequestMethod.GET)
    public
    @ResponseBody
    Map<String, Object> logout() {
        // 初始化 Response
        Response response = ResponseFactory.newInstance();

        Subject currentSubject = SecurityUtils.getSubject();
        if (currentSubject != null && currentSubject.isAuthenticated()) {
            // 执行账户注销操作
            currentSubject.logout();
            response.setResponseResult(Response.RESPONSE_RESULT_SUCCESS);
        } else {
            response.setResponseResult(Response.RESPONSE_RESULT_ERROR);
            response.setResponseMsg("did not login");
        }

        return response.generateResponse();
    }

    /**
     * 修改账户密码
     *
     * @param passwordInfo 密码信息
     * @param request      请求
     * @return 返回一个 Map 对象，其中键值为 result 代表修改密码操作的结果，
     * 值为 success 或 error；键值为 msg 代表需要返回给用户的信息
     */
    @RequestMapping(value = "passwordModify", method = RequestMethod.POST)
    public
    @ResponseBody
    Map<String, Object> passwordModify(@RequestBody Map<String, Object> passwordInfo,
                                       HttpServletRequest request) {
        //初始化 Response
        Response responseContent = ResponseFactory.newInstance();

        String errorMsg = null;
        String result = Response.RESPONSE_RESULT_ERROR;

        // 获取用户 ID
        HttpSession session = request.getSession();
        UserInfoDTO userInfo = (UserInfoDTO) session.getAttribute("userInfo");
        Integer userID = userInfo.getUserID();

        try {
            // 更改密码
            accountService.passwordModify(userID, passwordInfo);

            result = Response.RESPONSE_RESULT_SUCCESS;
        } catch (UserAccountServiceException e) {
            errorMsg = e.getExceptionDesc();
        }
        // 设置 Response
        responseContent.setResponseResult(result);
        responseContent.setResponseMsg(errorMsg);
        return responseContent.generateResponse();
    }

    /**
     * 获取图形验证码 将返回一个包含4位字符（字母或数字）的图形验证码，并且将图形验证码的值设置到用户的 session 中
     *
     * @param time     时间戳
     * @param response 返回的 HttpServletResponse 响应
     */
    @RequestMapping(value = "checkCode/{time}", method = RequestMethod.GET)
    public void getCheckCode(@PathVariable("time") String time, HttpServletResponse response, HttpServletRequest request) {

        BufferedImage checkCodeImage = null;
        String checkCodeString = null;

        // 获取图形验证码
        Map<String, Object> checkCode = CaptchaGenerator.generateCaptcha();

        if (checkCode != null) {
            checkCodeString = (String) checkCode.get("captchaString");
            checkCodeImage = (BufferedImage) checkCode.get("captchaImage");
        }

        if (checkCodeString != null && checkCodeImage != null) {
            try (ServletOutputStream outputStream = response.getOutputStream()) {
                // 设置 Session
                HttpSession session = request.getSession();
                session.setAttribute("checkCode", checkCodeString);

                // 将验证码输出
                ImageIO.write(checkCodeImage, "png", outputStream);

                response.setHeader("Pragma", "no-cache");
                response.setHeader("Cache-Control", "no-cache");
                response.setDateHeader("Expires", 0);
                response.setContentType("image/png");
            } catch (IOException e) {
                log.error("fail to get the ServletOutputStream");
            }
        }else{
        	log.error("fail to gettttttt the ServletOutputStream");
        }
    }
}
