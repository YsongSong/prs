package com.wymessi.controller;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.druid.util.StringUtils;
import com.wymessi.exception.CustomException;
import com.wymessi.param.GenerateApplyParam;
import com.wymessi.param.ProjectListParam;
import com.wymessi.po.Project;
import com.wymessi.po.SysUser;
import com.wymessi.service.ProjectService;
import com.wymessi.service.UserService;
import com.wymessi.utils.Result;

@Controller
@RequestMapping("/project")
public class ProjectController {

	@Autowired
	private ProjectService projectService;

	@Autowired
	private UserService userService;
	
	/**
	 * 项目申请页面
	 * 
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/uploadPage")
	public String uploadPage(HttpSession session) throws Exception {
		if (session.getAttribute("user") == null) {
			throw new CustomException("未登录，请先登录", "/prs/");
		}

		return "applicant/upload";
	}
	
	/**
	 * 项目分配页面
	 * 
	 * @return
	 * @throws Exception
	 */
	@RequestMapping("/allocatePage")
	public String allocatePage(HttpSession session) throws Exception {
		if (session.getAttribute("user") == null) {
			throw new CustomException("未登录，请先登录", "/prs/");
		}

		return "system/allocate";
	}


	/**
	 * 处理上传请求
	 * 
	 * @param model
	 * @param file
	 * @param session
	 * @param project
	 * @param tagsinput
	 *            领域标签用逗号隔开
	 * @return
	 */
	@RequestMapping("/upload")
	public String upload(Model model, MultipartFile file, HttpSession session, Project project, String tagsinput) {
		SysUser user = (SysUser) session.getAttribute("user");
		if (user == null) {
			throw new CustomException("未登录，请先登录", "/prs/");
		}
		// 生成申请记录
		GenerateApplyParam param = new GenerateApplyParam();
		param.setModel(model);
		param.setFile(file);
		param.setProject(project);
		param.setSysUser(user);
		param.setTagsinput(tagsinput);
		projectService.generateApply(param);

		return "applicant/upload";
	}

	@RequestMapping("/delete")
	public void delete(HttpSession session, Long id) {
		SysUser user = (SysUser) session.getAttribute("user");
		if (user == null) {
			throw new CustomException("未登录，请先登录", "/prs/");
		}
		// 删除记录
		projectService.deleteById(id);
	}

	/**
	 * 更新申请记录接口
	 * 
	 * @param model
	 * @param session
	 * @param project
	 * @return
	 */
	@ResponseBody
	@RequestMapping("/update")
	public Result<String> update(Model model, HttpSession session, Project project) {
		SysUser user = (SysUser) session.getAttribute("user");
		if (user == null) {
			throw new CustomException("未登录，请先登录", "/prs/");
		}
		Result<String> result = new Result<String>();
		Project p = projectService.getProjectById(project.getId());
		if (p == null) {
			result.setData("该申请记录不存在，请刷新页面重新查看");
			return result;
		}
		p.setProjectName(project.getProjectName());
		p.setDescription(project.getDescription());
		// 更新申请记录
		p.setLastUpdateTime(new Date());
		int rows = projectService.update(p); // 返回影响的行数
		if (rows > 0) {
			result.setData("修改成功");
		} else {
			result.setData("修改失败");
		}
		return result;
	}

	/**
	 * 查询项目申请记录接口，以json的形式返回
	 * 
	 * @param session
	 * @param request
	 * @return
	 */
	@ResponseBody
	@RequestMapping("/projects.json")
	public Map<String, Object> getProjectsJson(HttpSession session, HttpServletRequest request) {
		SysUser user = (SysUser) session.getAttribute("user");
		String projectName = request.getParameter("projectName");
		String status = request.getParameter("status");
		String createTime = request.getParameter("createTime");
		String isApplicant = request.getParameter("isApplicant");
		int limit = Integer.valueOf(request.getParameter("limit"));
		List<Long> createUserIds = null;
		int offset = (Integer.valueOf(request.getParameter("page")) - 1) * limit;
		// 生成查询参数
		ProjectListParam param = new ProjectListParam();
		if (!StringUtils.isEmpty(projectName))
			param.setProjectName(projectName);
		if (!StringUtils.isEmpty(status))
			param.setStatus(status);
		if (!StringUtils.isEmpty(isApplicant)){
			createUserIds = new ArrayList<Long>();
			createUserIds.add(user.getId());
			param.setCreateUserIds(createUserIds);
		}
		String createUserName = request.getParameter("username");
		if (!StringUtils.isEmpty(createUserName)) {
			createUserIds = userService.getUserByUserName(createUserName);
			param.setCreateUserIds(createUserIds);
		}
		
		// 设置正确的日期格式
		if (!StringUtils.isEmpty(createTime))
			setTimeRange(param, createTime);
		param.setOffset(offset);
		param.setLimit(limit);

		int totalCount = projectService.getTotalCount(param);
		List<Project> projects = projectService.listProject(param);
		// 因为layui数据表格的限制，故将数据转成表格需要的格式
		formatProject(projects);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("code", 0);
		map.put("count", totalCount);
		map.put("msg", "");
		map.put("data", projects);
		return map;
	}

	/**
	 * 设置正确的日期格式
	 * 
	 * @param param
	 * @param startToEndTime
	 * @throws ParseException
	 */
	private void setTimeRange(ProjectListParam param, String startToEndTime) {
		String[] strs = startToEndTime.split("~");
		for (String string : strs) {
			Date date = null;
			try {
				date = DateUtils.parseDate(string.trim(), "yyyy-MM-dd HH:mm:ss");
			} catch (ParseException e) {
				throw new RuntimeException("时间转化异常");
			}
			if (param.getStartTime() == null) {
				param.setStartTime(date);
			} else {
				param.setEndTime(date);
			}

		}
	}

	/**
	 * 因为layui数据表格的限制，故将数据转成表格需要的格式
	 * 
	 * @param projects
	 */
	private void formatProject(List<Project> projects) {
		for (Project project : projects) {
			project.setCreateUserName(project.getUser().getUsername());
			if (Project.PROJECT_REVIEW_STATUS_WAIT_ALLOCATE.equals(project.getStatus())) {
				project.setStatus("待分配");
			}
			if (Project.PROJECT_REVIEW_STATUS_WAIT_REVIEW.equals(project.getStatus())) {
				project.setStatus("待评审");
			}
			if (Project.PROJECT_REVIEW_STATUS_IN_REVIEW.equals(project.getStatus())) {
				project.setStatus("评审中");
			}
			if (Project.PROJECT_REVIEW_STATUS_REVIEW_DONG.equals(project.getStatus())) {
				project.setStatus("评审完成");
			}
		}
	}

}