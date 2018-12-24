package tbcloud.user.api.http;

import jframe.core.msg.PluginMsg;
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
import tbcloud.lib.api.util.IDUtil;
import tbcloud.lib.api.util.StringUtil;
import tbcloud.user.api.http.req.*;
import tbcloud.user.api.http.rsp.ImgCodeRsp;
import tbcloud.user.api.http.rsp.SignInRsp;
import tbcloud.user.model.UserImgCode;
import tbcloud.user.model.UserInfo;
import tbcloud.user.model.UserInfoExample;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 开放平台 用户相关接口
 *
 * @author dzh
 * @date 2018-11-08 20:21
 */
@Path("open/user")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OpenUserResource extends BaseResource {

    static Logger LOG = LoggerFactory.getLogger(OpenUserResource.class);

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
        data.setImgCodeUrl(String.join("/", Plugin.getConfig(ApiConst.QINIU_DOMAIN), imgCode.getImgPath()));
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
        if (StringUtil.isEmpty(mobile) || mobile.length() != 11) {//TODO 国际化
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
            Plugin.send(new PluginMsg<MobileVCode>().setType(MsgType.MOBILE_VCODE).setValue(msg));
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
        userInfo.setApikey(IDUtil.genApikeyV1(userInfo.getId()));
        userInfo.setRole(ApiConst.USER_ROLE_DEVELOPER);

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

        // validate imgCode
        if (!isValidImgCode(req.getImgCodeId(), req.getImgCode())) {
            r.setCode(ApiCode.INVALID_PARAM);
            r.setMsg("invalid param imgCode:" + req.getImgCode());
            return r;
        }

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
        setToRedis(ApiConst.REDIS_ID_USER, ApiConst.REDIS_KEY_OPEN_TOKEN_ + userInfo.getId(), token, ApiConst.REDIS_EXPIRED_1H * 12);

        // send UserLogin
        UserLogin msg = new UserLogin();
        msg.setPlatform(ApiConst.PLATFORM_OPEN);
        msg.setUserId(userInfo.getId());
        msg.setDate(System.currentTimeMillis());
        msg.setStatus(ApiConst.IS_ONLINE);
        msg.setToken(token);
        Plugin.send(new PluginMsg<UserLogin>().setType(MsgType.USER_LOGIN).setValue(msg));

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

        r.setData(reqContext.getUserInfo());
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

        Plugin.send(new PluginMsg<EmailModify>().setType(MsgType.EMAIL_MODIFY).setValue(em));
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
        deleteFromRedis(ApiConst.REDIS_ID_USER, ApiConst.REDIS_KEY_OPEN_TOKEN_ + userInfo.getId());

        // user_online
        UserLogin msg = new UserLogin();
        msg.setPlatform(ApiConst.PLATFORM_OPEN);
        msg.setUserId(userInfo.getId());
        msg.setDate(System.currentTimeMillis());
        msg.setStatus(ApiConst.IS_OFFLINE);
        Plugin.send(new PluginMsg<UserLogin>().setType(MsgType.USER_LOGIN).setValue(msg));

        return r;
    }

    public final int validateToken(ReqContext reqContext) {
        String token = reqContext.getToken();

        long usrId = IDUtil.decodeUserIDFromToken(token);
        if (usrId <= 0) return ApiCode.TOKEN_INVALID;

        UserInfo userInfo = UserDao.selectUserInfo(usrId);
        if (userInfo == null) return ApiCode.TOKEN_INVALID;
        reqContext.setUserInfo(userInfo); // update context

        String usrToken = getFromRedis(ApiConst.REDIS_ID_USER, ApiConst.REDIS_KEY_OPEN_TOKEN_ + usrId);
//        Jedis jedis = null;
//        try {
//            jedis = Jedis.getJedis(ApiConst.REDIS_ID_USER);
//            if (jedis == null) return ApiCode.REDIS_GET_NULL;
//
//            usrToken = jedis.get(ApiConst.REDIS_KEY_USER_TOKEN_ + usrId);
//        } finally {
//            if (jedis != null) Jedis.recycleJedis(ApiConst.REDIS_ID_USER, jedis);
//        }

        if (usrToken == null) return ApiCode.TOKEN_EXPIRED;
        if (!usrToken.equals(token)) return ApiCode.TOKEN_INVALID;

        return ApiCode.SUCC;
    }


}
