package org.javaboy.vhr.controller;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.javaboy.vhr.config.FastDFSUtils;
import org.javaboy.vhr.model.Hr;
import org.javaboy.vhr.model.RespBean;
import org.javaboy.vhr.service.HrService;
import org.javaboy.vhr.util.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.Map;

/**
 * @作者 江南一点雨
 * @公众号 江南一点雨
 * @微信号 a_java_boy
 * @GitHub https://github.com/lenve
 * @博客 http://wangsong.blog.csdn.net
 * @网站 http://www.javaboy.org
 * @时间 2020-03-01 13:07
 */
@RestController
public class HrInfoController {

    @Autowired
    HrService hrService;

    @Value("${fastdfs.nginx.host}")
    String nginxHost;

    @GetMapping("/hr/info")
    public Hr getCurrentHr(Authentication authentication) {
        return ((Hr) authentication.getPrincipal());
    }

    @PutMapping("/hr/info")
    public RespBean updateHr(@RequestBody Hr hr, Authentication authentication) {
        if (hrService.updateHr(hr) == 1) {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(hr, authentication.getCredentials(), authentication.getAuthorities()));
            return RespBean.ok("更新成功!");
        }
        return RespBean.error("更新失败!");
    }

    @PutMapping("/hr/pass")
    public RespBean updateHrPasswd(@RequestBody Map<String, Object> info) {
        String oldpass = (String) info.get("oldpass");
        String pass = (String) info.get("pass");
        Integer hrid = (Integer) info.get("hrid");
        if (hrService.updateHrPasswd(oldpass, pass, hrid)) {
            return RespBean.ok("更新成功!");
        }
        return RespBean.error("更新失败!");
    }

    @PostMapping("/hr/userface1")
    public RespBean updateHrUserface1(MultipartFile file, Integer id,Authentication authentication) throws IOException {
//        String fileId = FastDFSUtils.upload(file);
//        String fileName = file.getOriginalFilename();
//        FileOutputStream fo = new FileOutputStream("template/"+id+fileName);
//        BufferedOutputStream bo = new BufferedOutputStream(fo);
        byte[] bytes = hrService.fileToByte(file);

//        bo.write(bytes);
//        bo.flush();
//        String url = nginxHost + fileId;
//        FileUtil.saveFile("template/"+id+fileName,bytes);
//        String url = "G:\\Code\\vhr-master\\vhr\\template/"+id+fileName;
//        String url = new String(bytes, "utf-8");
        if (hrService.updateUserfaceByte(bytes, id) == 1) {
            Hr hr = (Hr) authentication.getPrincipal();
            hr.setUserface1(bytes);
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(hr, authentication.getCredentials(), authentication.getAuthorities()));
            return RespBean.ok("更新成功!", null);
        }
        return RespBean.error("更新失败!");
    }

//    @PostMapping("/hr/userface")
//    public RespBean updateHrUserface(MultipartFile file, Integer id,Authentication authentication) {
//        String fileId = FastDFSUtils.upload(file);
//        String url = nginxHost + fileId;
//        if (hrService.updateUserface(url, id) == 1) {
//            Hr hr = (Hr) authentication.getPrincipal();
//            hr.setUserface(url);
//            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(hr, authentication.getCredentials(), authentication.getAuthorities()));
//            return RespBean.ok("更新成功!", url);
//        }
//        return RespBean.error("更新失败!");
//    }
}