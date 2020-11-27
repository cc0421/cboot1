package com.lz.ht.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.lz.ht.model.Resources;
import com.lz.ht.model.Role;
import com.lz.ht.result.Result;
import com.lz.ht.model.Dept;
import com.lz.ht.model.User;
import com.lz.ht.page.PageModel;
import com.lz.ht.result.Result;
import com.lz.ht.service.DeptService;
import com.lz.ht.service.UserService;
import com.lz.ht.util.JwtUtil;
import com.lz.ht.base.BaseController;

import java.util.HashMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import com.google.gson.Gson;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import com.github.abel533.sql.SqlMapper;
@Slf4j
@Controller
public class DeptController extends BaseController{

    @Autowired
    private DeptService deptServiceImpl;

    @Autowired
    private UserService userServiceImpl;

    @RequestMapping(value = "/dept/list",method = {RequestMethod.GET})
    public String dept_list()throws Exception{
        return "dept/dept_list";
    }



//    @RequestMapping(value = "/dept/list",method = {RequestMethod.POST})
//    @ResponseBody
//    public PageModel list(Dept dept, PageModel<Dept> page)throws Exception{
//           page.init();
//           List<Dept> list = deptServiceImpl.findPageList(page,dept);
//           long count = deptServiceImpl.findCount(dept);
//           page.packData(count,list);
//           return page;
//    }
    @RequestMapping(value = "/dept/list",method = {RequestMethod.POST})
    @ResponseBody
    public PageModel list(Dept dept , PageModel<Dept> pageModel)throws Exception{
        pageModel.init();//分析返回的数据：pageSize  currentage
        //调用pageHelper
        PageHelper.startPage((int)pageModel.getCurrentPageNum(),(int)pageModel.getPageSize());
        //业务数据
        PageInfo<Dept> pageInfo =
                new PageInfo<Dept>(this.deptServiceImpl.findList(dept));

        //组装数据返回到客户端，使用的为springboot默认的Json包 spring-boot-starter-json 依赖：
        pageModel.packData(pageInfo.getTotal(),pageInfo.getList());
        return pageModel;
    }

    @RequestMapping(value = "/dept/add",method = {RequestMethod.GET})
    public String addInit(Dept dept,Model model){
        return "dept/dept_add";
    }

    @RequestMapping(value = "/dept/add",method = {RequestMethod.POST})
    @ResponseBody
    public Result add(Dept dept){
        deptServiceImpl.add(dept);
        return Result.genSuccessResult();
    }

    @RequestMapping(value = "/dept/update",method = {RequestMethod.GET})
    public String updateInit(Dept dept,Model model){
        dept = deptServiceImpl.findById(dept.getId());
        model.addAttribute("dept",dept);
        return "dept/dept_update";
    }

    @RequestMapping(value = "/dept/update",method = {RequestMethod.POST})
    @ResponseBody
    public Result update(Dept dept){
        deptServiceImpl.updateById(dept);
        return Result.genSuccessResult();
    }

    @RequestMapping(value = "/dept/delete",method = {RequestMethod.POST})
    @ResponseBody
    public Result delete(Dept dept){
    	User record = new User();
    	record.setDeptId(dept.getId());
    	//查询所有以此部门为部门的用户，若有，则提示不能删除
    	List<User> findList = userServiceImpl.findList(record);
        if(findList!=null && findList.size()>0) {
    		 return Result.genResult("998","本部门有用户在使用，不可删除！");
         } 
        deptServiceImpl.deleteById(dept.getId()); 
        return Result.genSuccessResult();
    }



    @Autowired
    private SqlMapper sqlMapper;

    private Gson gson = new Gson();

    @RequestMapping(value = "/dept/selectPageList",method = {RequestMethod.GET})
    public String selectPageList(){
        return "dept/dept_selectPageList";
    }

    @RequestMapping(value = "/dept/selectPageList",method = {RequestMethod.POST})
    @ResponseBody
    public PageModel selectPageList(Dept dept, PageModel<Dept> page)throws Exception{
        page.init();
        List<Dept> list = deptServiceImpl.findPageList(page,dept);
        long count = deptServiceImpl.findCount(dept);
        page.packData(count,list);
        return page;
    }


    @RequestMapping(value = "/dept/choosePage",method = {RequestMethod.GET})
    public String selectPage(String fkName,Model model){
        if(StringUtils.isNotEmpty(fkName)){
            Map<String, Object> foreignKeyMap = sqlMapper.selectOne("select * from t_fkeys where fkName = \'"+ fkName+"\'" );
            String rSql = foreignKeyMap.get("rSql").toString();
            log.info("rSql:{}",rSql);
            List<Map<String, Object>> relativeMapList = sqlMapper.selectList(rSql);
            if(relativeMapList!=null){
                model.addAttribute("relativeMapList",relativeMapList);
                String rType = foreignKeyMap.get("rType").toString();
                if("0".equals(rType)) return select_radioPage(model);
                if("1".equals(rType)) return select_radioPage(model);
                if("2".equals(rType)) {
                    String coverOtherValueColumn = foreignKeyMap.get("coverOtherValueColumn").toString();
                    model.addAttribute("coverOtherValueColumn",coverOtherValueColumn);
                    model.addAttribute("treeNodes",toJson(relativeMapList));
                    return select_zTreePage(model);
                }
            }else{
                return "error/error";
            }
        }
        return "error/error";
    }
//service层新建两个接口findRoleIdByUserId，findByresKey
//    @RequestMapping(value = "/dept/management",method = {RequestMethod.GET})
//    public String roleResourceInit(Role role, Model model) {
//        Subject subject = SecurityUtils.getSubject();
//        Session session = subject.getSession();
//        Long userId = (Long) session.getAttribute("loginUserId");
//
//        Long roleId = deptServiceImpl.findRoleIdByUserId(userId);
//        //String rolekey = (String) session.getAttribute("loginUserId");
//
//        role = roleServiceImpl.findById(roleId);
//        //根据role 查找roleResource表中，本角色管理的资源
//        //SELECT  t.id AS id, t.deptName AS `name`, t.parentId pId , 'true' AS `open` FROM t_dept t
//        String roleKey = role.getRoleKey();
//        //1.查询所有的资源列表
//        //2.查询角色对应的资源
//        //3.遍历所有资源列表，如果角色对应的资源列表中有，就给他标识为选中状态
//        String sql = "    SELECT  res.resKey AS id, " +
//                "    res.name AS name, " +
//                "    res.presKey AS pId, " +
//                "    'true' AS open,     " +
//                "    CASE WHEN   res.resKey IN ( SELECT r.resKey FROM t_role_resources r WHERE r.roleKey = "+
//                "  #{roleKey} )  THEN 'true' ELSE 'false' END AS checked  " +
//                " FROM  t_resources res ";
//
//        List<Map> selectList = sqlMapper.selectList(sql,  roleKey, Map.class);
//
//        model.addAttribute("treeNodes",toJson(selectList));
//        model.addAttribute("roleKey",roleKey);
//        return "resources/resources_management";
//    }
//
//    @RequestMapping(value = "/resources/managelist",method = {RequestMethod.GET})
//    @ResponseBody
//    public Map<String ,Object> manageList(int resKey){
//        Map<String, Object> modelMap = new HashMap<String, Object>();
//        Resources resources = resourcesServiceImpl.findByresKey((long) resKey);
//        System.out.println(resources);
//        //model.addAttribute("resources",resources);
//        modelMap.put("data", resources);
//        return modelMap;
//    }





    private String select_radioPage(Model model){
        model.addAttribute("demo","demoValue");
        return "dept/dept_selectPageList";
    }

    private String select_zTreePage(Model model){
        model.addAttribute("demo","demoValue");
        return "dept/dept_foreignKeyTree";
    }
    public String toJson(Object obj){
        String s = gson.toJson(obj);
        return s;
    }


}
