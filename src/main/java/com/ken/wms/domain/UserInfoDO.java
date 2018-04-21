package com.ken.wms.domain;

import java.io.Serializable;

/**
 * 用户账户信息(数据传输对象)
 * @author ken
 * @since 2017/2/26.
 */
public class UserInfoDO implements Serializable{

    /**
     * 用户ID
     */
    private Integer userID;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 用户密码（已加密）
     */
    private String password;

    /**
     * 是否为初次登陆
     */
    private int firstLogin;

    /**
     * 用户账户属性的 getter 以及 setter
     */

    public String getUserName() {
        return userName;
    }

    public int getFirstLogin() {
        return firstLogin;
    }

    public void setFirstLogin(int firstLogin) {
        this.firstLogin = firstLogin;
    }

    public Integer getUserID() {
        return userID;
    }

    public String getPassword() {
        return password;
    }

    public void setUserID(Integer userID) {
        this.userID = userID;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "UserInfoDO{" +
                "userID=" + userID +
                ", userName='" + userName + '\'' +
                ", password='" + password + '\'' +
                ", firstLogin=" + firstLogin +
                '}';
    }
}
