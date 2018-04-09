package com.wymessi.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.wymessi.dao.UserDao;
import com.wymessi.po.SysUser;
import com.wymessi.service.UserService;

/**
 * 系统用户业务层接口实现
 * 
 * @author 王冶
 *
 */
@Service
public class UserServiceImpl implements UserService {

	@Autowired
	private UserDao userDao;

	/**
	 * 项目申请者注册
	 * 
	 * @param applicant
	 */
	@Override
	public void registerApplicant(SysUser sysUser) {
		if (sysUser != null)
			userDao.registerApplicant(sysUser);
	}

	/**
	 * 项目申请者登陆
	 * 
	 * @param param
	 * @return
	 */
	@Override
	public SysUser login(SysUser sysUser) {
		if (sysUser != null) {
			SysUser user = userDao.login(sysUser);
			return user;
		}
		return null;
	}

}
