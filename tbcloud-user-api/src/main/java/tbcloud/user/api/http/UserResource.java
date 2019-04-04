package tbcloud.user.api.http;

import com.alibaba.druid.util.StringUtils;
import jframe.core.msg.PluginMsg;
import jframe.core.msg.TextMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tbcloud.lib.api.ApiCode;
import tbcloud.lib.api.ApiConst;
import tbcloud.lib.api.ApiField;
import tbcloud.lib.api.Result;
import tbcloud.lib.api.msg.EmailModify;
import tbcloud.lib.api.msg.MobileVCode;
import tbcloud.lib.api.msg.MsgType;
import tbcloud.lib.api.msg.UserLogin;
import tbcloud.lib.api.util.GsonUtil;
import tbcloud.lib.api.util.IDUtil;
import tbcloud.lib.api.util.StringUtil;
import tbcloud.node.model.*;
import tbcloud.node.model.ext.NodeInfoRt;
import tbcloud.node.model.util.NodeUtil;
import tbcloud.node.protocol.data.ins.Ins;
import tbcloud.node.protocol.data.ins.InsVal;
import tbcloud.user.api.http.req.*;
import tbcloud.user.api.http.rsp.*;
import tbcloud.user.model.*;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户平台
 *
 * @author dzh
 * @date 2018-11-08 20:20
 */
@Path("user")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserResource extends BaseResource {

    static Logger LOG = LoggerFactory.getLogger(UserResource.class);

    @POST
    @Path("imgcode/get")
    public Result<ImgCodeRsp> getImgCode(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version) {
        LOG.info("{} {}", ui.getPath(), version);
        Result<ImgCodeRsp> r = new Result<>();

        String imgCodeId = IDUtil.genImgCodeId(version); //TODO ip
        UserImgCode imgCode = UserDao.selectUserImgCode(IDUtil.innerImgCodeId(imgCodeId));

        if (imgCode == null) {
            r.setCode(ApiCode.INVALID_PARAM);
            r.setMsg("not found imgCode");
            return r;
        }

        ImgCodeRsp data = new ImgCodeRsp();
        data.setImgCodeId(imgCodeId);
        data.setImgCodeUrl(Qiniu.publicDownloadUrl(ApiConst.QINIU_ID_IMGCODE, imgCode.getImgPath()));
        r.setData(data);
        return r;
    }

    @POST
    @Path("vcode/get")
    public Result<String> getVCode(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, MobileVcodeReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, req.toString());
        Result<String> r = new Result<>();

        // validate imgCode
        if (!isValidImgCode(req.getImgCodeId(), req.getImgCode())) {
            r.setCode(ApiCode.INVALID_PARAM);
            r.setMsg("invalid param imgCode " + req.getImgCode());
            return r;
        }

        String mobile = req.getMobile();
        if (isInvalidMobile(mobile)) {
            r.setCode(ApiCode.INVALID_PARAM);
            r.setMsg("invalid mobile " + mobile);
            return r;
        }

        String vcode = getFromRedis(ApiConst.REDIS_ID_USER, ApiConst.REDIS_KEY_USER_VCODE_ + mobile);
        if (!StringUtil.isEmpty(vcode)) {
            r.setCode(ApiCode.OP_MORE_THAN_LIMIT);
            r.setMsg("request more than limit");
            return r;
        }

        vcode = IDUtil.genMobileVcode(4);
        if (Plugin.isDebug() && !Plugin.envName().equals(ApiConst.ENV_ONLINE)) {
            r.setData(vcode);
            r.setMsg("测试时返回");
        }

        if (!ApiConst.ENV_DEV.equals(Plugin.envName())) { // not send if dev
            MobileVCode msg = new MobileVCode();
            msg.setType(req.getType());
            msg.setMobile(mobile);
            msg.setCode(vcode);


            Plugin.sendToUser(new PluginMsg<String>().setType(MsgType.MOBILE_VCODE).setValue(GsonUtil.toJson(msg)));
            //Plugin.send(new PluginMsg<MobileVCode>().setType(MsgType.MOBILE_VCODE).setValue(msg));
        }

        // cache
        setToRedis(ApiConst.REDIS_ID_USER, ApiConst.REDIS_KEY_USER_VCODE_ + mobile, vcode, 60);

        LOG.info("send vcode {} to {}", vcode, req.getMobile());
        return r;
    }

    /**
     * FIXME 这是一个不安全的接口，会被刷短信
     *
     * @param ui
     * @param version
     * @param req
     * @return
     */
    @POST
    @Path("vcode/getv2")
    public Result<String> getVCode_v2(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, MobileVcodeReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, req.toString());
        Result<String> r = new Result<>();

        String mobile = req.getMobile();
        if (isInvalidMobile(mobile)) {
            r.setCode(ApiCode.INVALID_PARAM);
            r.setMsg("invalid mobile " + mobile);
            return r;
        }

        String vcode = getFromRedis(ApiConst.REDIS_ID_USER, ApiConst.REDIS_KEY_USER_VCODE_ + mobile);
        if (!StringUtil.isEmpty(vcode)) {
            r.setCode(ApiCode.OP_MORE_THAN_LIMIT);
            r.setMsg("request more than limit");
            return r;
        }

        vcode = IDUtil.genMobileVcode(4);
        if (Plugin.isDebug() && !Plugin.envName().equals(ApiConst.ENV_ONLINE)) {
            r.setData(vcode);
            r.setMsg("测试时返回");
        }

        if (!ApiConst.ENV_DEV.equals(Plugin.envName())) { // not send if dev
            MobileVCode msg = new MobileVCode();
            msg.setType(req.getType());//4
            msg.setMobile(mobile);
            msg.setCode(vcode);


            Plugin.sendToUser(new PluginMsg<String>().setType(MsgType.MOBILE_VCODE).setValue(GsonUtil.toJson(msg)));
            //Plugin.send(new PluginMsg<MobileVCode>().setType(MsgType.MOBILE_VCODE).setValue(msg));
        }

        // cache
        setToRedis(ApiConst.REDIS_ID_USER, ApiConst.REDIS_KEY_USER_VCODE_ + mobile, vcode, 60);

        LOG.info("send vcode {} to {}", vcode, req.getMobile());
        return r;
    }

    @POST
    @Path("signup/passwd")
    public Result<Void> passwdSignUp(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, SignUpPasswdReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, req.toString());
        Result<Void> r = new Result<>();

        String mobile = req.getMobile();
        String vcode = req.getVcode();


        if (StringUtil.isEmpty(mobile) || StringUtil.isEmpty(vcode) || StringUtil.isEmpty(req.getPasswd())) {
            r.setCode(ApiCode.HTTP_MISS_PARAM);
            return r;
        }

        // validate vcode
        if (!isValidVcode(mobile, vcode)) {
            r.setCode(ApiCode.INVALID_PARAM);
            r.setMsg("invalid param vcode:" + req.getVcode());
            return r;
        }

        // mobile must be not used  TODO opt
        UserInfoExample example = new UserInfoExample();
        example.createCriteria().andMobileEqualTo(mobile).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        List<UserInfo> userInfoList = UserDao.selectUserInfo(example);
        if (userInfoList != null && userInfoList.size() > 0) {
            r.setCode(ApiCode.DB_INSERT_ERROR);
            r.setMsg("mobile existed " + mobile);
            return r;
        }

        // insert user
        UserInfo userInfo = new UserInfo();
        userInfo.setMobile(req.getMobile());
        userInfo.setName(req.getName());
        //TODO userInfo.setInviteCode("");
        userInfo.setPasswd(StringUtil.MD5Encode(req.getPasswd() + ApiConst.USER_PASSWD_SALT_1));
        //userInfo.setApikey(IDUtil.genApikeyV1(userInfo.getId()));

        if (UserDao.insertUserInfo(userInfo) != 1) {
            r.setCode(ApiCode.DB_INSERT_ERROR);
            r.setMsg("signup unknow error " + mobile);
            return r;
        } else {
            LOG.info("create new userId:" + userInfo.getId());
        }
        return r;
    }


    @POST
    @Path("signup/vcode")
    public Result<SignInRsp> vcodeSignUp(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, SignInVCodeReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, req.toString());
        Result<SignInRsp> r = new Result<>();

        String mobile = req.getMobile();
        String vcode = req.getVcode();


        if (StringUtil.isEmpty(mobile) || StringUtil.isEmpty(vcode)) {
            r.setCode(ApiCode.HTTP_MISS_PARAM);
            return r;
        }

        // validate vcode
        if (!isValidVcode(mobile, vcode)) {
            r.setCode(ApiCode.INVALID_PARAM);
            r.setMsg("invalid param vcode:" + req.getVcode());
            return r;
        }

        // mobile must be not used  TODO opt
        UserInfoExample example = new UserInfoExample();
        example.createCriteria().andMobileEqualTo(mobile).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        List<UserInfo> userInfoList = UserDao.selectUserInfo(example);
        if (userInfoList != null && userInfoList.size() > 0) {
            r.setCode(ApiCode.DB_INSERT_ERROR);
            r.setMsg("mobile existed " + mobile);
            return r;
        }

        // insert user
        UserInfo userInfo = new UserInfo();
        userInfo.setMobile(req.getMobile());
        //userInfo.setName(req.getName());
        //TODO userInfo.setInviteCode("");
        //userInfo.setPasswd(StringUtil.MD5Encode(req.getPasswd() + ApiConst.USER_PASSWD_SALT_1));
        //userInfo.setApikey(IDUtil.genApikeyV1(userInfo.getId()));

        if (UserDao.insertUserInfo(userInfo) != 1) {
            r.setCode(ApiCode.DB_INSERT_ERROR);
            r.setMsg("signup unknow error " + mobile);
            return r;
        } else {
            LOG.info("create new userId:" + userInfo.getId());
        }

        // token
        String token = IDUtil.genToken(userInfo.getId());
        // save to redis
        setToRedis(ApiConst.REDIS_ID_USER, ApiConst.REDIS_KEY_USER_TOKEN_ + userInfo.getId(), token, ApiConst.REDIS_EXPIRED_1H * 12);

        SignInRsp rsp = new SignInRsp(); // 和SignInRsp相同
        rsp.setToken(token);
        r.setData(rsp);

        return r;
    }

    @POST
    @Path("signin/passwd")
    public Result<SignInRsp> passwdSignIn(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, SignInPasswdReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, req.toString());
        Result<SignInRsp> r = new Result<>();

        String mobile = req.getMobile();

        if (isInvalidMobile(mobile)) {
            r.setCode(ApiCode.INVALID_PARAM);
            r.setMsg("invalid mobile " + mobile);
            return r;
        }

        // validate imgCode TODO App没有验证机制去掉
//        if (!isValidImgCode(req.getImgCodeId(), req.getImgCode())) {
//            r.setCode(ApiCode.INVALID_PARAM);
//            r.setMsg("invalid param imgCode:" + req.getImgCode());
//            return r;
//        }

        // select user
        UserInfoExample example = new UserInfoExample();
        example.createCriteria().andMobileEqualTo(mobile)
                .andPasswdEqualTo(StringUtil.MD5Encode(req.getPasswd() + ApiConst.USER_PASSWD_SALT_1))
                .andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        List<UserInfo> userInfoList = UserDao.selectUserInfo(example);
        if (userInfoList == null || userInfoList.size() != 1) {
            r.setCode(ApiCode.DB_NOT_FOUND_RECORD);
            r.setMsg("check your passwd! not found user:" + mobile);
            return r;
        }

        UserInfo userInfo = userInfoList.get(0);
        String token = IDUtil.genToken(userInfo.getId());
        // save to redis
        setToRedis(ApiConst.REDIS_ID_USER, ApiConst.REDIS_KEY_USER_TOKEN_ + userInfo.getId(), token, ApiConst.REDIS_EXPIRED_1H * 12);

        // send UserLogin
        UserLogin msg = new UserLogin();
        msg.setPlatform(ApiConst.PLATFORM_USER);
        msg.setUserId(userInfo.getId());
        msg.setDate(System.currentTimeMillis());
        msg.setStatus(ApiConst.IS_ONLINE);
        msg.setToken(token);
        Plugin.sendToUser(new PluginMsg<String>().setType(MsgType.LOGIN_OUT).setValue(GsonUtil.toJson(msg)), userInfo.getId());
//        Plugin.send(new PluginMsg<UserLogin>().setType(MsgType.USER_LOGIN).setValue(msg));

        SignInRsp rsp = new SignInRsp();
        rsp.setToken(token);
        r.setData(rsp);
        return r;
    }

    @POST
    @Path("signin/vcode")
    public Result<SignInRsp> vcodeSignIn(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, SignInVCodeReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, req.toString());
        Result<SignInRsp> r = new Result<>();

        String mobile = req.getMobile();

        if (isInvalidMobile(mobile)) {
            r.setCode(ApiCode.INVALID_PARAM);
            r.setMsg("invalid mobile " + mobile);
            return r;
        }

        // check vcode
        String vcode = req.getVcode();
        if (StringUtil.isEmpty(vcode)) {
            r.setCode(ApiCode.INVALID_PARAM);
            r.setMsg("invalid vcode");
            return r;
        }
        // validate vcode
        if (!isValidVcode(mobile, vcode)) {
            r.setCode(ApiCode.INVALID_PARAM);
            r.setMsg("invalid vcode:" + req.getVcode());
            return r;
        }

        // select user
        UserInfoExample example = new UserInfoExample();
        example.createCriteria().andMobileEqualTo(mobile).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        List<UserInfo> userInfoList = UserDao.selectUserInfo(example);
        if (userInfoList == null || userInfoList.size() != 1) {
            r.setCode(ApiCode.DB_NOT_FOUND_RECORD);
            r.setMsg("check your mobile! not found user:" + mobile);
            return r;
        }

        UserInfo userInfo = userInfoList.get(0);
        String token = IDUtil.genToken(userInfo.getId());
        // save to redis
        setToRedis(ApiConst.REDIS_ID_USER, ApiConst.REDIS_KEY_USER_TOKEN_ + userInfo.getId(), token, ApiConst.REDIS_EXPIRED_1H * 12);

        // send UserLogin
        UserLogin msg = new UserLogin();
        msg.setPlatform(ApiConst.PLATFORM_USER);
        msg.setUserId(userInfo.getId());
        msg.setDate(System.currentTimeMillis());
        msg.setStatus(ApiConst.IS_ONLINE);
        msg.setToken(token);
        Plugin.sendToUser(new PluginMsg<String>().setType(MsgType.LOGIN_OUT).setValue(GsonUtil.toJson(msg)), userInfo.getId());
//        Plugin.send(new PluginMsg<UserLogin>().setType(MsgType.USER_LOGIN).setValue(msg));

        SignInRsp rsp = new SignInRsp();
        rsp.setToken(token);
        r.setData(rsp);
        return r;
    }

    @POST
    @Path("passwd/forget")
    public Result<Void> forgetPasswd(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, PasswdForgetReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, req.toString());
        Result<Void> r = new Result<>();

        String mobile = req.getMobile();
        String vcode = req.getVcode();
        String passwd = req.getPasswd();

        if (StringUtil.isEmpty(mobile) || StringUtil.isEmpty(vcode) || StringUtil.isEmpty(passwd)) {
            r.setCode(ApiCode.HTTP_MISS_PARAM);
            return r;
        }

        // validate vcode
        if (!isValidVcode(mobile, vcode)) {
            r.setCode(ApiCode.INVALID_PARAM);
            r.setMsg("invalid param vcode:" + req.getVcode());
            return r;
        }

        // update passwd
        UserInfoExample example = new UserInfoExample();
        example.createCriteria().andMobileEqualTo(mobile).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        List<UserInfo> userInfoList = UserDao.selectUserInfo(example);
        if (userInfoList == null || userInfoList.size() != 1) {
            r.setCode(ApiCode.DB_NOT_FOUND_RECORD);
            r.setMsg("check your passwd! not found user:" + mobile);
            return r;
        }

        UserInfo userInfo = userInfoList.get(0);
        userInfo.setPasswd(StringUtil.MD5Encode(passwd + ApiConst.USER_PASSWD_SALT_1));
        if (UserDao.updateUserInfo(userInfo) != 1) {
            r.setCode(ApiCode.DB_UPDATE_ERROR);
            r.setMsg("forgot passwd reset error. mobile:" + mobile);
            return r;
        }
        return r;
    }

    @POST
    @Path("passwd/reset")
    public Result<Void> resetPasswd(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, PasswdResetReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, req.toString());
        Result<Void> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        String passwd = req.getPasswd();

        UserInfo userInfo = reqContext.getUserInfo();

        UserInfo newUserInfo = new UserInfo();
        newUserInfo.setId(userInfo.getId());
        newUserInfo.setPasswd(StringUtil.MD5Encode(passwd + ApiConst.USER_PASSWD_SALT_1));
        if (UserDao.updateUserInfo(newUserInfo) != 1) {
            r.setCode(ApiCode.DB_UPDATE_ERROR);
            r.setMsg("forgot passwd reset error. userId:" + userInfo.getId());
            return r;
        }

        return r;
    }

    @POST
    @Path("info/get")
    public Result<UserInfo> getInfo(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<UserInfo> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        UserInfo userInfo = reqContext.getUserInfo();
        if (!StringUtil.isEmpty(userInfo.getAvatar()))
            userInfo.setAvatar(Qiniu.privateDownloadUrl(ApiConst.QINIU_ID_USER, userInfo.getAvatar(), -1));
        r.setData(userInfo);
        return r;
    }

    @POST
    @Path("info/getext")
    public Result<UserInfoRsp> getExtInfo(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, UserReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<UserInfoRsp> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        UserInfo userInfo = reqContext.getUserInfo();
        if (!StringUtil.isEmpty(userInfo.getAvatar()))
            userInfo.setAvatar(Qiniu.privateDownloadUrl(ApiConst.QINIU_ID_USER, userInfo.getAvatar(), -1));

        UserInfoRsp data = new UserInfoRsp();
        data.setUser(userInfo);

        Integer filter = req.getFilter();
        if (filter != null) {
            if ((filter & 1) > 0) { // user_node
                UserNodeExample example = new UserNodeExample();
                example.createCriteria().andUserIdEqualTo(userInfo.getId()).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
                List<UserNode> list = UserDao.selectUserNode(example);

                data.setNodeSize(list == null ? 0 : list.size());
            }
        }

        r.setData(data);
        return r;
    }

    @POST
    @Path("info/set")
    public Result<UserInfo> setInfo(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, UserInfoSetReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<UserInfo> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        UserInfo userInfo = reqContext.getUserInfo();

        UserInfo newUserInfo = new UserInfo();
        newUserInfo.setId(userInfo.getId());
        newUserInfo.setName(req.getName());
        newUserInfo.setAvatar(req.getAvatar());
        UserDao.updateUserInfo(newUserInfo);

        // update user_node
        UserNodeExample example = new UserNodeExample();
        example.createCriteria().andUserIdEqualTo(userInfo.getId()).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        UserNode updated = new UserNode();
        //updated.setUserName(req.getName());
        updated.setUserAvatar(req.getAvatar());
        UserDao.updateUserNodeSelective(updated, example);

        return r;
    }

    @POST
    @Path("mobile/verify")
    public Result<UserInfo> verifyMobile(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, MobileVerifyReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<UserInfo> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        String mobile = req.getMobile();
        String vcode = req.getVcode();

        if (StringUtil.isEmpty(mobile) || StringUtil.isEmpty(vcode)) {
            r.setCode(ApiCode.HTTP_MISS_PARAM);
            return r;
        }

        // validate vcode
        if (!isValidVcode(mobile, vcode)) {
            r.setCode(ApiCode.INVALID_PARAM);
            r.setMsg("invalid param vcode:" + req.getVcode());
            return r;
        }

        // update mobile
        UserInfo userInfo = reqContext.getUserInfo();
        String oldMobile = userInfo.getMobile();
        LOG.info("mobile verify from {} to {}", oldMobile, mobile);

        UserInfo newUserInfo = new UserInfo();
        newUserInfo.setId(userInfo.getId());
        newUserInfo.setMobile(mobile);
        UserDao.updateUserInfo(newUserInfo); //TODO

        return r;
    }


    /**
     * 请求修改email,将异步发送确认邮件到新邮箱。通过用verifyEmail认证后修改成功
     *
     * @param ui
     * @param version
     * @param token
     * @param req
     * @return
     */
    @POST
    @Path("email/modify")
    public Result<String> modifyEmail(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, Map<String, String> req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<String> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        // TODO 验证必要参数

        UserInfo userInfo = reqContext.getUserInfo();
        String name = userInfo.getName();


        String email = req.get(ApiField.F_email);
        String emailToken = IDUtil.genEmailToken(userInfo.getId());

        if (Plugin.isDebug() && !Plugin.envName().equals(ApiConst.ENV_ONLINE)) {
            r.setData(emailToken);
            r.setMsg("测试时返回");
        }

        // email saved into redis
        setToRedis(ApiConst.REDIS_ID_USER, ApiConst.REDIS_KEY_USER_EMAIL_TOKEN_ + emailToken, email, ApiConst.REDIS_EXPIRED_1H);

        // async to send email
        EmailModify em = new EmailModify();
        em.setEmail(email);
        em.setName(Optional.ofNullable(name).orElse(userInfo.getMobile()));
        em.setToken(emailToken);

        Plugin.sendToUser(new PluginMsg<String>().setType(MsgType.EMAIL_MODIFY).setValue(GsonUtil.toJson(em)), userInfo.getId());
//        Plugin.send(new PluginMsg<EmailModify>().setType(MsgType.EMAIL_MODIFY).setValue(em));
        return r;
    }

    @POST
    @Path("email/verify")
    public Result<Void> verifyEmail(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, Map<String, String> req) {
        LOG.info("{} {} {}", ui.getPath(), version, req);
        Result<Void> r = new Result<>();

        String emailToken = req.get(ApiField.F_token);
        String passwd = req.get(ApiField.F_passwd);
        if (StringUtil.isEmpty(emailToken) || StringUtil.isEmpty(passwd)) {
            r.setCode(ApiCode.HTTP_MISS_PARAM);
            return r;
        }

        //TODO limit req

        // retrieve email info from redis,token maybe expired
        String email = getFromRedis(ApiConst.REDIS_ID_USER, ApiConst.REDIS_KEY_USER_EMAIL_TOKEN_ + emailToken);
        if (StringUtil.isEmpty(email)) {
            r.setCode(ApiCode.TOKEN_EXPIRED);
            r.setMsg("邮件链接已经超过有效期，请重新发起邮件修改请求");
            return r;
        }

        long userId = IDUtil.decodeUserIDFromEmailToken(emailToken);
        UserInfo userInfo = UserDao.selectUserInfo(userId);
        if (userInfo == null) {
            r.setCode(ApiCode.TOKEN_INVALID);
            r.setMsg("邮件链接包含无效的参数，请联系树熊云");
            return r;
        }

        // TODO 最多允许输入3次
        passwd = StringUtil.MD5Encode(passwd + ApiConst.USER_PASSWD_SALT_1);
        if (!passwd.equals(userInfo.getPasswd())) {
            r.setCode(ApiCode.USR_INVALID);
            r.setMsg("请输入正确的用户密码");
            return r;
        }

        // update email
        UserInfo updateEmail = new UserInfo();
        updateEmail.setId(userId);
        updateEmail.setEmail(email);
        UserDao.updateUserInfo(updateEmail);

//        try {
//            Response.seeOther(new URI("")); //
//        } catch (URISyntaxException e) {
//            LOG.error(e.getMessage(), e);
//        }
        return r;
    }

    @POST
    @Path("node/family/join")
    public Result<Void> joinNodeFamily(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, UserNodeReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<Void> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        UserInfo userInfo = reqContext.getUserInfo();

        String inviteCode = req.getInviteCode();
        if (StringUtil.isEmpty(inviteCode)) {
            r.setCode(ApiCode.HTTP_MISS_PARAM);
            r.setMsg("miss inviteCode");
            return r;
        }

        NodeInfoExample example = new NodeInfoExample();
        example.createCriteria().andInviteCodeEqualTo(inviteCode).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        List<NodeInfo> nodeList = NodeDao.selectNodeInfo(example);
        if (nodeList == null || nodeList.isEmpty()) {
            r.setCode(ApiCode.DB_NOT_FOUND_RECORD);
            r.setMsg("not found node");
            return r;
        }

        NodeInfo node = nodeList.get(0);

        // check existed
        UserNodeExample userNodeExample = new UserNodeExample();
        userNodeExample.createCriteria().andNodeIdEqualTo(node.getNodeId()).andUserIdEqualTo(userInfo.getId()).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        long existedUser = UserDao.countUserNode(userNodeExample);
        if (existedUser > 0) {
            r.setCode(ApiCode.USR_INVALID);
            r.setMsg(userInfo.getName() + " has joined");
            return r;
        }

        // check max family users
        UserNodeExample countExample = new UserNodeExample();
        countExample.createCriteria().andNodeIdEqualTo(node.getNodeId()).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        existedUser = UserDao.countUserNode(countExample);
        if (existedUser >= 20) {
            r.setCode(ApiCode.USR_INVALID);
            r.setMsg("too many users");
            return r;
        }

        UserNode userNode = new UserNode();
        userNode.setUserId(userInfo.getId());
        userNode.setUserAvatar(userInfo.getAvatar());
        userNode.setUserName(userInfo.getName());
        userNode.setUserMobile(userInfo.getMobile());
        userNode.setRole(ApiConst.USER_NODE_ROLE_ADMIN);

        userNode.setNodeId(node.getNodeId());
        userNode.setNodeName(node.getName());

        UserDao.insertUserNode(userNode);

        return r;
    }

    @POST
    @Path("node/family/set")
    public Result<Void> setNodeFamily(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, UserNodeReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<Void> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        UserInfo userInfo = reqContext.getUserInfo();

        String nodeId = req.getNodeId();
        if (StringUtil.isEmpty(nodeId)) {
            r.setCode(ApiCode.HTTP_MISS_PARAM);
            r.setMsg("miss nodeId");
            return r;
        }

        UserNodeExample example = new UserNodeExample();
        example.createCriteria().andNodeIdEqualTo(nodeId).andUserIdEqualTo(userInfo.getId()).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);

        UserNode userNode = new UserNode();
        userNode.setNodeName(req.getUserName());

        UserDao.updateUserNodeSelective(userNode, example);

        return r;
    }

    @POST
    @Path("node/family/list")
    public Result<PageRsp<UserNode>> listNodeFamily(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, UserNodeReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<PageRsp<UserNode>> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        UserInfo userInfo = reqContext.getUserInfo();

        String nodeId = req.getNodeId();
        if (StringUtil.isEmpty(nodeId)) {
            r.setCode(ApiCode.HTTP_MISS_PARAM);
            r.setMsg("miss nodeId");
            return r;
        }

        UserNodeExample example = new UserNodeExample();
        example.createCriteria().andNodeIdEqualTo(nodeId).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        example.setOrderByClause("nodeId limit 1000"); //max 1000

        List<UserNode> userNodeList = UserDao.selectUserNode(example);
        if (userNodeList != null) {
            userNodeList.forEach(node -> {
                if (!StringUtil.isEmpty(node.getUserAvatar()))
                    node.setUserAvatar(Qiniu.privateDownloadUrl(ApiConst.QINIU_ID_USER, node.getUserAvatar(), -1));
            });
        }

        PageRsp<UserNode> data = new PageRsp<>();
        data.setPage(userNodeList);
        data.setTotal(userNodeList == null ? 0 : userNodeList.size());

        r.setData(data);
        return r;
    }

    @POST
    @Path("node/family/quit")
    public Result<Void> quitNodeFamily(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, UserNodeReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<Void> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        UserInfo userInfo = reqContext.getUserInfo();

        String nodeId = req.getNodeId();
        if (StringUtil.isEmpty(nodeId)) {
            r.setCode(ApiCode.HTTP_MISS_PARAM);
            r.setMsg("miss nodeId");
            return r;
        }

        Long deletedUser = req.getUserId();
        if (deletedUser == null) {
            r.setCode(ApiCode.HTTP_MISS_PARAM);
            r.setMsg("miss userId");
            return r;
        }

        // check role is 0
        UserNodeExample example = new UserNodeExample();
        example.createCriteria().andNodeIdEqualTo(nodeId).andUserIdEqualTo(userInfo.getId())
                .andRoleEqualTo(ApiConst.USER_NODE_ROLE_ROOT).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        List<UserNode> userNodeList = UserDao.selectUserNode(example);
        if (userNodeList != null && !userNodeList.isEmpty()) {

            if (deletedUser == userInfo.getId()) { //root不能删除自己
                r.setCode(ApiCode.USR_INVALID);
                r.setMsg("invalid user");
                return r;
            }

            UserNode deleted = new UserNode();
            deleted.setIsDelete(ApiConst.IS_DELETE_Y);

            UserNodeExample deletedExample = new UserNodeExample();
            deletedExample.createCriteria().andNodeIdEqualTo(nodeId).andUserIdEqualTo(deletedUser);

            UserDao.updateUserNodeSelective(deleted, deletedExample);
        }

        return r;
    }

    @POST
    @Path("node/device/list")
    public Result<PageRsp<NodeDevice>> listNodeDevice(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, NodeReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<PageRsp<NodeDevice>> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        UserInfo userInfo = reqContext.getUserInfo();

        String nodeId = req.getNodeId();
        if (StringUtil.isEmpty(nodeId)) {
            r.setCode(ApiCode.HTTP_MISS_PARAM);
            r.setMsg("miss nodeId");
            return r;
        }

        Integer pageNo = req.getPageNo();
        Integer pageSize = req.getPageSize();

        NodeDeviceExample example = new NodeDeviceExample();
        example.createCriteria().andNodeIdEqualTo(nodeId).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        long total = NodeDao.countNodeDevice(example);

        example.setOrderByClause("update_time desc limit " + (pageNo - 1) * pageSize + "," + pageSize);
        List<NodeDevice> devices = NodeDao.selectNodeDevice(example);
        devices.forEach(dev -> {
            dev.setMacIcon(Qiniu.privateDownloadUrl(ApiConst.QINIU_ID_USER, dev.getMacIcon(), -1));
        });

        PageRsp<NodeDevice> data = new PageRsp<>();
        data.setTotal(total);
        data.setPage(devices);

        r.setData(data);
        return r;
    }

    @POST
    @Path("node/device/set")
    public Result<Void> setNodeDevice(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, NodeDevice req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<Void> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        UserInfo userInfo = reqContext.getUserInfo();

        String nodeId = req.getNodeId();
        if (StringUtil.isEmpty(nodeId)) {
            r.setCode(ApiCode.HTTP_MISS_PARAM);
            r.setMsg("miss nodeId");
            return r;
        }

        String mac = req.getMac();
        if (StringUtil.isEmpty(mac)) {
            r.setCode(ApiCode.HTTP_MISS_PARAM);
            r.setMsg("miss mac");
            return r;
        }

        Plugin.sendToUser(new TextMsg().setType(MsgType.NODE_DEVICE_BLOCK).setValue(GsonUtil.toJson(req)), userInfo.getId());
        return r;
    }

    @POST
    @Path("node/bind")
    public Result<Void> bindNode(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, NodeReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<Void> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        UserInfo userInfo = reqContext.getUserInfo();

        String nodeId = req.getNodeId();
        if (StringUtil.isEmpty(nodeId)) {
            r.setCode(ApiCode.HTTP_MISS_PARAM);
            r.setMsg("miss nodeId");
            return r;
        }

//        NodeInfo nodeInfo = NodeDao.selectNodeInfo(nodeId);
//        if (nodeInfo == null) { //insert
//            nodeInfo = new NodeInfo();
//            nodeInfo.setNodeId(nodeId);
//            nodeInfo.setBindTime(System.currentTimeMillis());
//            nodeInfo.setIsBind(NodeConst.IS_BIND);
//            nodeInfo.setUserId(userInfo.getId());
//            if (NodeDao.insertNodeInfo(nodeInfo) != 1) {
//                r.setCode(ApiCode.DB_INSERT_ERROR);
//                r.setMsg(nodeId + " bind error!");
//                return r;
//            }
//        } else {  //update
//            if (nodeInfo.getIsBind() == NodeConst.IS_BIND) {
//                r.setCode(ApiCode.INVALID_PARAM);
//                r.setMsg(nodeId + " be bound!");
//                return r;
//            }
//
//            Long ownUserId = nodeInfo.getUserId();
//            if (ownUserId != null && ownUserId > 0 && ownUserId != userInfo.getId()) {
//                r.setCode(ApiCode.USR_INVALID);
//                r.setMsg(nodeId + " be bound!");
//                return r;
//            }
//
//            NodeInfo rebindNode = new NodeInfo();
//            rebindNode.setNodeId(nodeId);
//            rebindNode.setBindTime(System.currentTimeMillis());
//            rebindNode.setIsBind(NodeConst.IS_BIND);
//            rebindNode.setUserId(userInfo.getId());
//            if (NodeDao.updateNodeInfo(rebindNode) != 1) {
//                r.setCode(ApiCode.DB_UPDATE_ERROR);
//                r.setMsg(nodeId + " bind error!");
//                return r;
//            }
//        }
//
//        // upsert node_rt
//        NodeRt nodeRt = NodeDao.selectNodeRt(nodeId);
//        if (nodeRt == null) {
//            nodeRt = new NodeRt();
//            nodeRt.setNodeId(nodeId);
//            //nodeRt.setStatus(NodeConst.STATUS_OFFLINE);
//            nodeRt.setUserId(userInfo.getId());
//            NodeDao.insertNodeRt(nodeRt);
//        } else {
//            NodeRt rebindNode = new NodeRt();
//            rebindNode.setNodeId(nodeId);
//            rebindNode.setUserId(userInfo.getId());
//            //rebindNode.setStatus(NodeConst.STATUS_OFFLINE);
//            NodeDao.updateNodeRt(rebindNode);
//        }

        // insert user_node
        UserNodeExample example = new UserNodeExample();
        example.createCriteria().andNodeIdEqualTo(nodeId).andRoleEqualTo(ApiConst.USER_NODE_ROLE_ROOT).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        List<UserNode> relatedList = UserDao.selectUserNode(example);
        if (relatedList != null && !relatedList.isEmpty()) {
            r.setCode(ApiCode.USR_INVALID);
            r.setMsg(nodeId + " be bound!");
            return r;
        }

        UserNode userNode = new UserNode();
        userNode.setUserId(userInfo.getId());
        userNode.setUserName(userInfo.getName());
        userNode.setUserMobile(userInfo.getMobile());
        userNode.setUserAvatar(userInfo.getAvatar());
        userNode.setNodeId(nodeId);
        userNode.setNodeName(req.getName());
        userInfo.setRole(ApiConst.USER_NODE_ROLE_ROOT);

        if (UserDao.insertUserNode(userNode) == 1) {
            NodeInfo nodeInfo = new NodeInfo();
            nodeInfo.setNodeId(nodeId);
            nodeInfo.setName(req.getName());
            nodeInfo.setIsBind(NodeConst.IS_BIND);
            nodeInfo.setBindTime(System.currentTimeMillis());
            nodeInfo.setUserId(userInfo.getId());

            NodeRt nodeRt = new NodeRt();
            nodeRt.setNodeId(nodeId);
            nodeRt.setUserId(userInfo.getId());

            NodeInfo existed = NodeDao.selectNodeInfo(nodeId);
            if (existed == null) {
                nodeInfo.setInviteCode(IDUtil.genNodeInviteCode());

                NodeDao.insertNodeInfo(nodeInfo);
                NodeDao.insertNodeRt(nodeRt);
            } else {
                NodeDao.updateNodeInfo(nodeInfo);
                NodeDao.updateNodeRt(nodeRt);
            }
        }

        return r;
    }

    /**
     * TODO 需要重新写逻辑
     *
     * @param ui
     * @param version
     * @param token
     * @param req
     * @return
     */
    @POST
    @Path("node/bindbatch")
    public Result<Void> bindBatchNode(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, Map<String, String> req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<Void> r = new Result<>();

        //
        r.setCode(ApiCode.ERROR_UNKNOWN);
        r.setMsg("deprecated");
        return r;

//        ReqContext reqContext = ReqContext.create(version, token);
//        r.setCode(validateToken(reqContext));
//        if (r.getCode() != ApiCode.SUCC) {
//            return r;
//        }
//        UserInfo userInfo = reqContext.getUserInfo();
//
//        // TODO
//        String fileType = req.get(ApiField.F_fileType);
//        String file = req.get(ApiField.F_file);
//        if (StringUtil.isEmpty(fileType) || StringUtil.isEmpty(file)) {
//            r.setCode(ApiCode.HTTP_MISS_PARAM);
//            r.setMsg("miss param");
//            return r;
//        }
//
//        switch (fileType) { //TODO aysnc to insert or limit req
//            case "1": { //换行符
//                String[] nodeIdList = file.split("[\r\n]+");
//                if (nodeIdList.length < 1 || nodeIdList.length > 1000) { //TODO config
//                    r.setCode(ApiCode.INVALID_PARAM);
//                    r.setMsg("nodes must be less than 1000");
//                    return r;
//                }
//
//                // 去重
//                Set<NodeInfo> nodeInfoList = new HashSet<>(nodeIdList.length);
//                Set<NodeRt> nodeRtList = new HashSet<>(nodeIdList.length);
//
//                long bindTime = System.currentTimeMillis();
//                for (int i = 0; i < nodeIdList.length; ++i) {
//                    String nodeId = nodeIdList[i].trim();
//                    if (StringUtil.isEmpty(nodeId)) continue; //skip empty nodeId
//
//                    if (NodeDao.selectNodeInfo(nodeId) != null) continue;  // 去重
//
//                    NodeInfo nodeInfo = new NodeInfo();
//                    nodeInfo.setNodeId(nodeId);
//                    nodeInfo.setBindTime(bindTime);
//                    nodeInfo.setIsBind(NodeConst.IS_BIND);
//                    nodeInfo.setUserId(userInfo.getId());
//
//                    nodeInfoList.add(nodeInfo);
//
//                    // node rt
//                    NodeRt nodeRt = new NodeRt();
//                    nodeRt.setNodeId(nodeId);
//                    nodeRt.setStatus(NodeConst.STATUS_OFFLINE);
//                    nodeRt.setUserId(userInfo.getId());
//                    nodeRtList.add(nodeRt);
//                }
//
//                if (nodeInfoList.isEmpty()) {
//                    r.setCode(ApiCode.INVALID_PARAM);
//                    r.setMsg("won't add any new node");
//                    return r;
//                }
//
//                if (NodeDao.batchInsertNodeInfo(nodeInfoList)) {
//                    NodeDao.batchInsertNodeRt(nodeRtList);
//                } else {
//                    r.setCode(ApiCode.DB_INSERT_ERROR);
//                    r.setMsg("batch insert error! check nodes' numbers");
//                    return r;
//                }
//                break;
//            }
//        }
//
//        return r;
    }

    @POST
    @Path("node/unbind")
    public Result<Void> unbindNode(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, Map<String, String> req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<Void> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        UserInfo userInfo = reqContext.getUserInfo();

        String nodeId = req.get(ApiField.F_nodeId);
        if (StringUtil.isEmpty(nodeId)) {
            r.setCode(ApiCode.HTTP_MISS_PARAM);
            r.setMsg("缺少请求参数");
            return r;
        }

        NodeInfo nodeInfo = NodeDao.selectNodeInfo(nodeId);
        if (nodeInfo == null) {
            r.setCode(ApiCode.DB_NOT_FOUND_RECORD);
            r.setMsg(nodeId + "没有找到");
            return r;
        }

        if (nodeInfo.getIsBind() == NodeConst.IS_UNBIND) {
            r.setCode(ApiCode.DB_UPDATE_ERROR);
            r.setMsg(nodeId + "已经解绑!");
            return r;
        }

        if (nodeInfo.getUserId() != userInfo.getId()) {
            r.setCode(ApiCode.USR_INVALID);
            r.setMsg("非法绑定");
            return r;
        }

        NodeInfo unbindNode = new NodeInfo();
        unbindNode.setNodeId(nodeId);
        unbindNode.setIsBind(NodeConst.IS_UNBIND);
        unbindNode.setUnbindTime(System.currentTimeMillis());
        unbindNode.setUserId(0L);
        NodeDao.updateNodeInfo(unbindNode);

        // update node_rt
        NodeRt nodeRt = new NodeRt();
        nodeRt.setNodeId(nodeId);
        //nodeRt.setStatus(NodeConst.STATUS_UNKNOWN);
        nodeRt.setUserId(0L);
        NodeDao.updateNodeRt(nodeRt);

        // delete user_node
        UserNodeExample example = new UserNodeExample();
        example.createCriteria().andNodeIdEqualTo(nodeId).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        UserNode deleted = new UserNode();
        deleted.setNodeId(nodeId);
        deleted.setIsDelete(ApiConst.IS_DELETE_Y);
        UserDao.updateUserNodeSelective(deleted, example);

        return r;
    }

    @POST
    @Path("node/select")
    public Result<Void> selectNode(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, NodeReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<Void> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        UserInfo userInfo = reqContext.getUserInfo();

        String selectedNode = req.getNodeId();
        if (StringUtil.isEmpty(selectedNode)) {
            r.setCode(ApiCode.HTTP_MISS_PARAM);
            r.setMsg("miss nodeId");
            return r;
        }

        UserNodeExample example = new UserNodeExample();
        example.createCriteria().andUserIdEqualTo(userInfo.getId()).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        List<UserNode> nodes = UserDao.selectUserNode(example);
        if (nodes != null && !nodes.isEmpty()) {
            List<UserNode> updated = new ArrayList<>(nodes.size());

            nodes.forEach(node -> {
                UserNode up = new UserNode();
                up.setId(node.getId());
                up.setIsSelect(node.getNodeId().equals(selectedNode) ? ApiConst.IS_SELECT_Y : ApiConst.IS_SELECT_N);

                updated.add(up);
            });

            UserDao.batchUpdateUserNode(updated);
        }

        return r;
    }

    @POST
    @Path("node/device/week/list")
    public Result<PageRsp<DeviceWeekRsp>> listNodeDeviceWeek(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, DeviceWeekReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<PageRsp<DeviceWeekRsp>> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        String nodeId = req.getNodeId();
        if (StringUtils.isEmpty(nodeId)) {
            r.setCode(ApiCode.HTTP_MISS_PARAM);
            r.setMsg("miss nodeId");
            return r;
        }

        UserInfo userInfo = reqContext.getUserInfo();

        Calendar c = Calendar.getInstance();

        Integer year = req.getYear();
        if (year == null) {
            year = c.get(Calendar.YEAR);
        }

        Integer week = req.getWeek();
        if (week == null) {
            week = c.get(Calendar.WEEK_OF_YEAR) - 1;
        }

        Integer pageNo = req.getPageNo();
        Integer pageSize = req.getPageSize();

        NodeDeviceWeekExample example = new NodeDeviceWeekExample();
        example.createCriteria().andNodeIdEqualTo(nodeId).andYearEqualTo(year).andWeekEqualTo(week).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        long count = NodeDao.countNodeDeviceWeek(example);

        example.setOrderByClause("nodeId limit" + (pageNo - 1) * pageSize + "," + pageSize);
        List<NodeDeviceWeek> deviceWeeks = NodeDao.selectNodeDeviceWeek(example);

        if (deviceWeeks != null) {
            List<DeviceWeekRsp> rspList = new ArrayList<>();
            deviceWeeks.forEach(deviceWeek -> {
                rspList.add(DeviceWeekRsp.from(deviceWeek));
            });

            PageRsp<DeviceWeekRsp> page = new PageRsp<>();
            page.setPage(rspList);
            page.setTotal(count);
            r.setData(page);
        }

        return null;
    }

    @POST
    @Path("node/listall")
    public Result<PageRsp<NodeInfoRt>> listAllNode(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, PageReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<PageRsp<NodeInfoRt>> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        UserInfo userInfo = reqContext.getUserInfo();

        Integer pageNo = req.getPageNo();
        Integer pageSize = req.getPageSize();

        NodeRtInfoExample example = new NodeRtInfoExample();
        NodeRtInfoExample.Criteria exampleCritera = example.createCriteria();

        NodeRtExample countExample = new NodeRtExample(); //TODO opt
        NodeRtExample.Criteria countExampleCriteria = countExample.createCriteria();

        UserNodeExample userNodeExample = new UserNodeExample();
        userNodeExample.createCriteria().andUserIdEqualTo(userInfo.getId()).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        userNodeExample.setOrderByClause("is_select desc limit " + (pageNo - 1) * pageSize + "," + pageSize);
        List<UserNode> familyNode = UserDao.selectUserNode(userNodeExample);
        if (familyNode == null || familyNode.isEmpty()) {
            PageRsp<NodeInfoRt> data = new PageRsp<>();
            data.setTotal(0L);
            r.setData(data);
            return r;
        }

        UserNode selectedNode = null;
        List<String> nodeIds = new ArrayList<>();
        for (UserNode userNode : familyNode) {
            if (!nodeIds.contains(userNode.getNodeId())) {
                nodeIds.add(userNode.getNodeId());
            }
            if (userNode.getIsSelect() == ApiConst.IS_SELECT_Y)
                selectedNode = userNode;
        }

        exampleCritera.andNodeIdIn(nodeIds);  // 假设一个人几个节点
        countExampleCriteria.andNodeIdIn(nodeIds);
        // not delete
        exampleCritera.andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        countExampleCriteria.andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        // page
        //example.setOrderByClause("create_time desc limit " + (pageNo - 1) * pageSize + "," + pageSize);

        List<NodeInfoRt> nodeList = NodeDao.selectNodeRtLeftJoinInfo(example);
        long count = NodeDao.countNodeRt(countExample); //TODO cache

        // firmwareU
        if (nodeList != null) {
            List<String> models = new ArrayList<>();
            for (NodeInfoRt info : nodeList) {
                if (!models.contains(info.getModel())) {
                    models.add(info.getModel());
                }
                info.setIsSelect((selectedNode != null && info.getNodeId().equals(selectedNode.getNodeId())) ?
                        ApiConst.IS_SELECT_Y : ApiConst.IS_SELECT_N);
            }

            List<NodeFirmware> firmwareList = new ArrayList<>(models.size());
            models.forEach(m -> {
                NodeFirmwareExample firmwareExample = new NodeFirmwareExample();
                firmwareExample.createCriteria().andModelEqualTo(m).andStartTimeGreaterThanOrEqualTo(System.currentTimeMillis())
                        .andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
                firmwareExample.setOrderByClause("start_time desc limit 1");
                List<NodeFirmware> latestFirmware = NodeDao.selectNodeFirmware(firmwareExample);
                if (latestFirmware != null && latestFirmware.size() > 0)
                    firmwareList.add(latestFirmware.get(0));
            });

            nodeList.forEach(info -> {
                NodeFirmware newF = null;
                for (NodeFirmware f : firmwareList) {
                    if (f.getModel().equals(info.getModel())) {
                        newF = f;
                        break;
                    }
                }
                if (newF != null) {
                    if (NodeUtil.compareFireware(newF.getFirmware(), info.getFirmware()) > 0) {
                        info.setFirmwareUpgrade(newF.getFirmware());
                    }
                }
            });
        }

        PageRsp<NodeInfoRt> data = new PageRsp<>();
        data.setTotal(count);
        data.setPage(nodeList);
        r.setData(data);
        return r;
    }

    @POST
    @Path("node/list")
    public Result<PageRsp<NodeInfoRt>> listNode(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, NodeListReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<PageRsp<NodeInfoRt>> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        UserInfo userInfo = reqContext.getUserInfo();

        Integer pageNo = req.getPageNo();
        Integer pageSize = req.getPageSize();

        NodeRtInfoExample example = new NodeRtInfoExample();
        NodeRtInfoExample.Criteria exampleCritera = example.createCriteria();

        NodeRtExample countExample = new NodeRtExample(); //TODO opt
        NodeRtExample.Criteria countExampleCriteria = countExample.createCriteria();

        // userId
        exampleCritera.andUserIdEqualTo(userInfo.getId());
        countExampleCriteria.andUserIdEqualTo(userInfo.getId());
        // nodeId
        if (!StringUtil.isEmpty(req.getNodeId())) {
            exampleCritera.andNodeIdLike(req.getNodeId() + "%");
            countExampleCriteria.andNodeIdLike(req.getNodeId() + "%");
        }
        // status
        List<Integer> status = req.getStatus();
        if (status != null && status.size() > 0) {
            exampleCritera.andStatusIn(status);
            countExampleCriteria.andStatusIn(status);
        }
        // not delete
        exampleCritera.andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        countExampleCriteria.andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        // page
        example.setOrderByClause("create_time desc limit " + (pageNo - 1) * pageSize + "," + pageSize);

        List<NodeInfoRt> nodeList = NodeDao.selectNodeRtLeftJoinInfo(example);
        long count = NodeDao.countNodeRt(countExample); //TODO cache

        // firmwareUpgrade
        if (nodeList != null) {
            List<String> models = new ArrayList<>();
            nodeList.forEach(info -> {
                if (!models.contains(info.getModel()))
                    models.add(info.getModel());
            });

            List<NodeFirmware> firmwareList = new ArrayList<>(models.size());
            models.forEach(m -> {
                NodeFirmwareExample firmwareExample = new NodeFirmwareExample();
                firmwareExample.createCriteria().andModelEqualTo(m).andStartTimeGreaterThanOrEqualTo(System.currentTimeMillis())
                        .andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
                firmwareExample.setOrderByClause("start_time desc limit 1");
                List<NodeFirmware> latestFirmware = NodeDao.selectNodeFirmware(firmwareExample);
                if (latestFirmware != null && latestFirmware.size() > 0)
                    firmwareList.add(latestFirmware.get(0));
            });

            nodeList.forEach(info -> {
                NodeFirmware newF = null;
                for (NodeFirmware f : firmwareList) {
                    if (f.getModel().equals(info.getModel())) {
                        newF = f;
                        break;
                    }
                }
                if (newF != null) {
                    if (NodeUtil.compareFireware(newF.getFirmware(), info.getFirmware()) > 0) {
                        info.setFirmwareUpgrade(newF.getFirmware());
                    }
                }
            });
        }

        PageRsp<NodeInfoRt> data = new PageRsp<>();
        data.setTotal(count);
        data.setPage(nodeList);
        r.setData(data);
        return r;
    }

    @POST
    @Path("message/list")
    public Result<PageRsp<UserMessage>> listMessage(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, PageReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<PageRsp<UserMessage>> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }


        UserInfo userInfo = reqContext.getUserInfo();

        Integer pageNo = req.getPageNo();
        Integer pageSize = req.getPageSize();

        UserMessageExample example = new UserMessageExample();
        example.createCriteria().andUserIdEqualTo(userInfo.getId()).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        example.setOrderByClause("create_time desc limit " + (pageNo - 1) * pageSize + "," + pageSize);

        List<UserMessage> list = UserDao.selectUserMessage(example);

        PageRsp<UserMessage> data = new PageRsp<>();
        data.setTotal(list.size());
        data.setPage(list);
        r.setData(data);
        return r;
    }

    @POST
    @Path("node/wifi/timer/get")
    public Result<NodeWifiTimer> getNodeWifiTimer(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, NodeReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<NodeWifiTimer> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        String nodeId = req.getNodeId();
        if (StringUtil.isEmpty(nodeId)) {
            r.setCode(ApiCode.INVALID_PARAM);
            r.setMsg("nodeId is nil");
            return r;
        }

        NodeWifiTimer timer = NodeDao.selectNodeWifiTimer(nodeId);
        if (timer == null) {
            timer = new NodeWifiTimer();
            timer.setNodeId(nodeId);
            timer.setOp(InsVal.OP_DISABLE);
        }
        r.setData(timer);
        return r;
    }

    @POST
    @Path("node/wifi/timer/set")
    public Result<Void> setNodeWifiTimer(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, NodeWifiTimerReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<Void> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        UserInfo userInfo = reqContext.getUserInfo();

        String nodeId = req.getNodeId();
        if (StringUtil.isEmpty(nodeId)) {
            r.setCode(ApiCode.INVALID_PARAM);
            r.setMsg("nodeId is nil");
            return r;
        }

        NodeWifiTimer timer = new NodeWifiTimer();
        timer.setNodeId(nodeId);
        timer.setOp(req.getOp());
        timer.setWifi(req.getWifi());

        Plugin.sendToUser(new TextMsg().setType(MsgType.NODE_WIFI_TIMER).setValue(GsonUtil.toJson(timer)), userInfo.getId());
        return r;
    }

    @POST
    @Path("node/wifi/list")
    public Result<PageRsp<NodeWifi>> listNodeWifi(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, Map<String, String> req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<PageRsp<NodeWifi>> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        String nodeId = req.get(ApiField.F_nodeId);
        if (StringUtil.isEmpty(nodeId)) {
            r.setCode(ApiCode.INVALID_PARAM);
            r.setMsg("nodeId is nil");
            return r;
        }

        UserInfo userInfo = reqContext.getUserInfo();

        Integer pageNo = 1;
        Integer pageSize = 10;

        NodeWifiExample example = new NodeWifiExample();
        example.createCriteria().andNodeIdEqualTo(nodeId).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        example.setOrderByClause("create_time desc limit " + (pageNo - 1) * pageSize + "," + pageSize);

        List<NodeWifi> list = NodeDao.selectNodeWifi(example);

        PageRsp<NodeWifi> data = new PageRsp<>();
        data.setTotal(list.size());
        data.setPage(list);
        r.setData(data);
        return r;
    }

    @POST
    @Path("node/device/allow/set")
    public Result<Void> setNodeDeviceAllow(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, NodeDeviceAllowReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<Void> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        UserInfo userInfo = reqContext.getUserInfo();

        String nodeId = req.getNodeId();
        if (StringUtil.isEmpty(nodeId)) {
            r.setCode(ApiCode.INVALID_PARAM);
            r.setMsg("nodeId is nil");
            return r;
        }

        // max 100
        NodeDeviceAllowExample example = new NodeDeviceAllowExample();
        example.createCriteria().andNodeIdEqualTo(nodeId).andOpEqualTo(1).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        long count = NodeDao.countNodeDeviceAllow(example);
        if (count > 100) {
            r.setCode(ApiCode.OP_MORE_THAN_LIMIT);
            r.setMsg("too many config");
            return r;
        }

        List<String> mac = req.getMac(); //mac 最多10个
        if (mac != null && mac.size() > 10) {
            r.setCode(ApiCode.INVALID_PARAM);
            r.setMsg("mac size more than 10");
            return r;
        }

        Plugin.sendToUser(new TextMsg().setType(MsgType.NODE_DEVICE_ALLOW)
                .setValue(GsonUtil.toJson(req.toNodeDeviceAllow())), userInfo.getId());
        return r;
    }

    @POST
    @Path("node/device/allow/list")
    public Result<PageRsp<NodeDeviceAllowRsp>> listNodeDeviceAllow(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, NodeReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<PageRsp<NodeDeviceAllowRsp>> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        UserInfo userInfo = reqContext.getUserInfo();

        String nodeId = req.getNodeId();
        if (StringUtil.isEmpty(nodeId)) {
            r.setCode(ApiCode.INVALID_PARAM);
            r.setMsg("nodeId is nil");
            return r;
        }

        // max 100
        NodeDeviceAllowExample example = new NodeDeviceAllowExample();
        example.createCriteria().andNodeIdEqualTo(nodeId).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        example.setOrderByClause("update_time desc limit 100");
        List<NodeDeviceAllow> page = NodeDao.selectNodeDeviceAllow(example);

        PageRsp<NodeDeviceAllowRsp> data = new PageRsp<>();
        data.setTotal(page.size());
        if (page != null) {
            data.setPage(page.stream().map(allow -> {
                return NodeDeviceAllowRsp.from(allow);
            }).collect(Collectors.toList()));
        }
        r.setData(data);

        return r;
    }

    @POST
    @Path("node/device/allow/del")
    public Result<Void> delNodeDeviceAllow(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, NodeDeviceAllow req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<Void> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        UserInfo userInfo = reqContext.getUserInfo();

        String nodeId = req.getNodeId();
        if (StringUtil.isEmpty(nodeId)) {
            r.setCode(ApiCode.INVALID_PARAM);
            r.setMsg("nodeId is nil");
            return r;
        }

        Long id = req.getId();
        if (id == null || id == 0) {
            r.setCode(ApiCode.INVALID_PARAM);
            r.setMsg("id is nil");
            return r;
        }

        NodeDeviceAllow deleted = new NodeDeviceAllow();
        deleted.setId(id);
        deleted.setOp(InsVal.OP_DISABLE);
        deleted.setIsDelete(ApiConst.IS_DELETE_Y);

        Plugin.sendToUser(new TextMsg().setType(MsgType.NODE_DEVICE_ALLOW).setValue(GsonUtil.toJson(req)), userInfo.getId());
        return r;
    }


    @POST
    @Path("node/wifi/set")
    public Result<Void> setNodeWifi(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, NodeWifiReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<Void> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        UserInfo userInfo = reqContext.getUserInfo();

        String nodeId = req.getNodeId();
        if (StringUtil.isEmpty(nodeId)) {
            r.setCode(ApiCode.INVALID_PARAM);
            r.setMsg("nodeId is nil");
            return r;
        }

        UserNodeExample userNodeExample = new UserNodeExample();
        userNodeExample.createCriteria().andNodeIdEqualTo(nodeId).andUserIdEqualTo(userInfo.getId()).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        List<UserNode> userNodeList = UserDao.selectUserNode(userNodeExample);
        if (userNodeList == null || userNodeList.isEmpty()) {
            r.setCode(ApiCode.USR_INVALID);
            r.setMsg("invalid user");
            return r;
        }

        Integer freq = req.getFreq();
        if (freq == null) {
            freq = ApiConst.WIFI_FREQ_ALL;
        }

        NodeWifi updated = new NodeWifi();
        if (!StringUtil.isEmpty(req.getSsid())) {
            updated.setSsid(req.getSsid());
        }
        if (!StringUtil.isEmpty(req.getPasswd())) {
            updated.setPasswd(req.getPasswd());
        }
        if (req.getRssi() != null) {
            updated.setFreq(req.getRssi());
        }

        NodeWifiExample example = new NodeWifiExample();
        if (freq == ApiConst.WIFI_FREQ_ALL) {
            example.createCriteria().andNodeIdEqualTo(nodeId).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        } else if (freq > 0) {
            example.createCriteria().andNodeIdEqualTo(nodeId).andFreqEqualTo(freq).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        }
        NodeDao.updateNodeWifiSelective(updated, example);
        // TODO send msg to notify node

        return r;
    }

    /**
     * TODO 通过redis防止多次请求
     *
     * @param ui
     * @param version
     * @param token
     * @param req
     * @return
     */
    @POST
    @Path("node/firmware/upgrade")
    public Result<Void> upgradeNodeFirmware(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, NodeReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<Void> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        UserInfo userInfo = reqContext.getUserInfo();

        String nodeId = req.getNodeId();
        if (StringUtil.isEmpty(nodeId)) {
            r.setCode(ApiCode.HTTP_MISS_PARAM);
            r.setMsg("nodeId is nil");
            return r;
        }

        NodeInfo nodeInfo = NodeDao.selectNodeInfo(nodeId);
        String model = nodeInfo.getModel();

        NodeFirmwareExample firmwareExample = new NodeFirmwareExample();
        firmwareExample.createCriteria().andModelEqualTo(model).andStartTimeGreaterThanOrEqualTo(System.currentTimeMillis())
                .andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        firmwareExample.setOrderByClause("start_time desc limit 1");
        List<NodeFirmware> latestFirmware = NodeDao.selectNodeFirmware(firmwareExample);
        if (latestFirmware == null || latestFirmware.size() <= 0) {
            r.setCode(ApiCode.NODE_FIRMWARE_LATEST);
            r.setMsg("node's firmware is latest");
            return r;
        }
        String newF = latestFirmware.get(0).getFirmware();
        if (NodeUtil.compareFireware(newF, nodeInfo.getFirmware()) < 1) {
            r.setCode(ApiCode.NODE_FIRMWARE_LATEST);
            r.setMsg("node's firmware is latest");
            return r;
        }

        // 正在升级的提示, 查询指令表
        NodeInsExample insExample = new NodeInsExample();
        insExample.createCriteria().andNodeIdEqualTo(nodeId).andInsEqualTo(Ins.INS_FIRMWAREUPGRADE)
                .andStatusLessThanOrEqualTo(NodeConst.INS_STATUS_RECV).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        List<NodeIns> list = NodeDao.selectNodeIns(insExample);
        if (list != null && !list.isEmpty()) {
            r.setCode(ApiCode.NODE_FIRMWARE_UPGRADING);
            r.setMsg("node is upgrading");
            return r;
        }

        Plugin.sendToUser(new TextMsg().setType(MsgType.NODE_FIRMWARE_UPGRADE).setValue(nodeId), userInfo.getId());
        return r;
    }

    @POST
    @Path("node/info/set")
    public Result<Void> setNodeInfo(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, NodeReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<Void> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        UserInfo userInfo = reqContext.getUserInfo();
        String nodeId = req.getNodeId();
        if (StringUtil.isEmpty(nodeId)) {
            r.setCode(ApiCode.HTTP_MISS_PARAM);
            r.setMsg("miss nodeId");
            return r;
        }

        NodeInfo nodeInfo = NodeDao.selectNodeInfo(nodeId);
        if (nodeInfo == null) {
            r.setCode(ApiCode.DB_NOT_FOUND_RECORD);
            r.setMsg(nodeId + " not found");
            return r;
        }

        if (nodeInfo.getIsBind() != NodeConst.IS_BIND) {
            r.setCode(ApiCode.DB_UPDATE_ERROR);
            r.setMsg(nodeId + " is not binded");
            return r;
        }

//        if (nodeInfo.getUserId() != userInfo.getId()) {
//            r.setCode(ApiCode.USR_INVALID);
//            r.setMsg("cann't not unbind others' node");
//            return r;
//        }

        UserNodeExample userNodeExample = new UserNodeExample();
        userNodeExample.createCriteria().andNodeIdEqualTo(nodeId).andUserIdEqualTo(userInfo.getId()).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        List<UserNode> userNodeList = UserDao.selectUserNode(userNodeExample);
        if (userNodeList == null || userNodeList.isEmpty()) {
            r.setCode(ApiCode.USR_INVALID);
            r.setMsg("invalid user");
            return r;
        }

        String name = req.getName();
        if (!StringUtil.isEmpty(name)) {
            NodeInfo updated = new NodeInfo();
            updated.setNodeId(nodeId);
            updated.setName(name);
            NodeDao.updateNodeInfo(updated);

            UserNode userNode = new UserNode();
            userNode.setNodeName(name);
            UserNodeExample example = new UserNodeExample();
            example.createCriteria().andNodeIdEqualTo(nodeId).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
            UserDao.updateUserNodeSelective(userNode, example);
        }
        return r;
    }


    @POST
    @Path("node/share/join")
    public Result<Void> joinShareNode(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, Map<String, String> req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<Void> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        UserInfo userInfo = reqContext.getUserInfo();
        String nodeId = req.get("nodeId");
        if (StringUtil.isEmpty(nodeId)) {
            r.setCode(ApiCode.HTTP_MISS_PARAM);
            r.setMsg("miss nodeId");
            return r;
        }

        NodeInfo nodeInfo = NodeDao.selectNodeInfo(nodeId);
        if (nodeInfo == null) {
            r.setCode(ApiCode.DB_NOT_FOUND_RECORD);
            r.setMsg(nodeId + " not found");
            return r;
        }

        if (nodeInfo.getIsBind() != NodeConst.IS_BIND) {
            r.setCode(ApiCode.DB_UPDATE_ERROR);
            r.setMsg(nodeId + " is not binded");
            return r;
        }

        if (nodeInfo.getUserId() != userInfo.getId()) {
            r.setCode(ApiCode.USR_INVALID);
            r.setMsg("cann't not unbind others' node");
            return r;
        }

        if (nodeInfo.getIsShare() == NodeConst.IS_SHARE) {
            r.setCode(ApiCode.DB_UPDATE_ERROR);
            r.setMsg(nodeId + " is shared");
            return r;
        }

        NodeInfo shareNode = new NodeInfo();
        shareNode.setNodeId(nodeId);
        shareNode.setUserId(userInfo.getId());
        shareNode.setIsShare(NodeConst.IS_SHARE);
        shareNode.setShareTime(System.currentTimeMillis());

        Plugin.sendToUser(new PluginMsg<String>().setType(MsgType.NODE_JOIN_SHARE).setValue(GsonUtil.toJson(shareNode)), userInfo.getId());
//        Plugin.send(new PluginMsg<NodeInfo>().setType(MsgType.NODE_JOIN_SHARE).setValue(shareNode));

        return r;
    }

    @POST
    @Path("node/share/quit")
    public Result<Void> quitShareNode(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, Map<String, String> req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<Void> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        UserInfo userInfo = reqContext.getUserInfo();
        String nodeId = req.get("nodeId");
        if (StringUtil.isEmpty(nodeId)) {
            r.setCode(ApiCode.HTTP_MISS_PARAM);
            r.setMsg("miss nodeId");
            return r;
        }

        NodeInfo nodeInfo = NodeDao.selectNodeInfo(nodeId);
        if (nodeInfo == null) {
            r.setCode(ApiCode.DB_NOT_FOUND_RECORD);
            r.setMsg(nodeId + " not found");
            return r;
        }

        if (nodeInfo.getIsShare() == NodeConst.IS_UNSHARE) {
            r.setCode(ApiCode.DB_UPDATE_ERROR);
            r.setMsg(nodeId + "is not shared");
            return r;
        }

        if (nodeInfo.getUserId() != userInfo.getId()) {
            r.setCode(ApiCode.USR_INVALID);
            r.setMsg("cann't not unbind others' node");
            return r;
        }

        NodeInfo shareNode = new NodeInfo();
        shareNode.setUserId(userInfo.getId());
        shareNode.setNodeId(nodeId);
        shareNode.setIsShare(NodeConst.IS_UNSHARE);
        shareNode.setUnshareTime(System.currentTimeMillis());

        Plugin.sendToUser(new PluginMsg<String>().setType(MsgType.NODE_QUIT_SHARE).setValue(GsonUtil.toJson(shareNode)), userInfo.getId());
//        Plugin.send(new PluginMsg<NodeInfo>().setType(MsgType.NODE_QUIT_SHARE).setValue(shareNode));
        return r;
    }


    @POST
    @Path("quit")
    public Result<Void> quit(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<Void> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        UserInfo userInfo = reqContext.getUserInfo();

        // rm token
        //deleteFromRedis(ApiConst.REDIS_ID_USER, ApiConst.REDIS_KEY_USER_TOKEN_ + userInfo.getId());

        // user_online
        UserLogin msg = new UserLogin();
        msg.setPlatform(ApiConst.PLATFORM_USER);
        msg.setUserId(userInfo.getId());
        msg.setDate(System.currentTimeMillis());
        msg.setStatus(ApiConst.IS_OFFLINE);

        Plugin.sendToUser(new PluginMsg<String>().setType(MsgType.LOGIN_OUT).setValue(GsonUtil.toJson(msg)), userInfo.getId());
//        Plugin.send(new PluginMsg<UserLogin>().setType(MsgType.USER_LOGIN).setValue(msg));

        return r;
    }


    @POST
    @Path("share/stat/sum")
    public Result<ShareSumRsp> sumStatShare(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<ShareSumRsp> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        UserInfo userInfo = reqContext.getUserInfo();

        UserShareSum shareSum = UserDao.selectUserShareSum(userInfo.getId());
        if (shareSum == null) {
            r.setCode(ApiCode.DB_NOT_FOUND_RECORD);
            r.setMsg("不存在共享收益记录");
            return r;
        }

        ShareSumRsp data = new ShareSumRsp();
        data.setSum(shareSum.getSum());
        data.setBalance(shareSum.getBalance());
        if (shareSum.getLatestTime() != null && (System.currentTimeMillis() - shareSum.getLatestTime()) < 48 * 3600 * 1000)
            data.setYesterday(shareSum.getLatest());
        r.setData(data);
        return r;
    }

    @POST
    @Path("share/stat/day")
    public Result<PageRsp<UserShareDay>> dayStatShare(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token, DatePageReq req) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<PageRsp<UserShareDay>> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        Long startTime = req.getStartTime();
        Long endTime = req.getEndTime();
        if (startTime == null || endTime == null) {
            r.setCode(ApiCode.HTTP_MISS_PARAM);
            r.setMsg("缺少请求参数");
            return r;
        }

        Long period = endTime - startTime;
        if (period < 0) {
            r.setCode(ApiCode.INVALID_PARAM);
            r.setMsg("非法请求参数");
            return r;
        }
//        if(period>  90 * 24 * 3600 * 1000){ //TODO 不能超过90
//        }

        UserInfo userInfo = reqContext.getUserInfo();

        Integer pageNo = req.getPageNo();
        Integer pageSize = req.getPageSize();

        UserShareDayExample example = new UserShareDayExample();
        example.createCriteria().andUserIdEqualTo(userInfo.getId()).andDateBetween(startTime, endTime).andIsDeleteEqualTo(ApiConst.IS_DELETE_N);
        example.setOrderByClause("date desc limit " + (pageNo - 1) * pageSize + "," + pageSize);
        List<UserShareDay> page = UserDao.selectUserShareDay(example);
        long count = UserDao.countUserShareDay(example);

        PageRsp<UserShareDay> data = new PageRsp<>();
        data.setPage(page);
        data.setTotal(count);
        r.setData(data);
        return r;
    }

    @POST
    @Path("qiniu/get")
    public Result<Map<String, String>> getQiniu(@Context UriInfo ui, @HeaderParam(ApiConst.API_VERSION) String version, @HeaderParam(ApiConst.API_TOKEN) String token) {
        LOG.info("{} {} {}", ui.getPath(), version, token);
        Result<Map<String, String>> r = new Result<>();

        ReqContext reqContext = ReqContext.create(version, token);
        r.setCode(validateToken(reqContext));
        if (r.getCode() != ApiCode.SUCC) {
            return r;
        }

        UserInfo userInfo = reqContext.getUserInfo();

        // TODO limit req
        Map<String, String> data = new HashMap<>(1, 1);
        data.put("token", Qiniu.uploadToken(ApiConst.QINIU_ID_USER, null));
        //data.put("bucket", Qiniu.info(ApiConst.QINIU_ID_DEVELOPER, QiniuConfig.BUCKET));
        r.setData(data);
        return r;
    }

    public final int validateToken(ReqContext reqContext) {
        String token = reqContext.getToken();

        long usrId = IDUtil.decodeUserIDFromToken(token);
        if (usrId <= 0) return ApiCode.TOKEN_INVALID;

        UserInfo userInfo = UserDao.selectUserInfo(usrId);
        if (userInfo == null) return ApiCode.TOKEN_INVALID;
        reqContext.setUserInfo(userInfo); // update context

        if (Plugin.envName().equals(ApiConst.ENV_DEV)) return ApiCode.SUCC;  // 方便测试
        String usrToken = getFromRedis(ApiConst.REDIS_ID_USER, ApiConst.REDIS_KEY_USER_TOKEN_ + usrId);
        if (usrToken == null) return ApiCode.TOKEN_EXPIRED;
        if (!usrToken.equals(token)) return ApiCode.TOKEN_INVALID;

        return ApiCode.SUCC;
    }
}