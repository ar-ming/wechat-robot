package me.biezhi.wechat.service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blade.kit.DateKit;
import com.blade.kit.FileKit;
import com.blade.kit.StringKit;
import com.blade.kit.http.HttpRequest;
import com.blade.kit.json.JSONArray;
import com.blade.kit.json.JSONKit;
import com.blade.kit.json.JSONObject;

import me.biezhi.wechat.Constant;
import me.biezhi.wechat.exception.WechatException;
import me.biezhi.wechat.model.WechatContact;
import me.biezhi.wechat.model.WechatMeta;
import me.biezhi.wechat.util.Matchers;
import me.biezhi.wechat.util.Md5Util;
import me.biezhi.wechat.util.PingUtil;

public class WechatServiceImpl implements WechatService {

	private static final Logger LOGGER = LoggerFactory.getLogger(WechatService.class);

	// 茉莉机器人
	// private Robot robot = new MoLiRobot();

	private static AtomicInteger ai = new AtomicInteger(0);

	/**
	 * 获取联系人
	 */
	@Override
	public void getContact(WechatMeta wechatMeta) throws WechatException {
		String url = wechatMeta.getBase_uri() + "/webwxgetcontact?pass_ticket=" + wechatMeta.getPass_ticket() + "&skey="
				+ wechatMeta.getSkey() + "&r=" + DateKit.getCurrentUnixTime();

		JSONObject body = new JSONObject();
		body.put("BaseRequest", wechatMeta.getBaseRequest());

		HttpRequest request = HttpRequest.post(url).contentType("application/json;charset=utf-8")
				.header("Cookie", wechatMeta.getCookie()).send(body.toString());

		LOGGER.debug(request.toString());
		String res = request.body();
		request.disconnect();

		if (StringKit.isBlank(res)) {
			LOGGER.error("获取联系人失败");
			throw new WechatException("获取联系人失败");
		}

		LOGGER.debug(res);

		try {
			JSONObject jsonObject = JSONKit.parseObject(res);
			JSONObject BaseResponse = jsonObject.get("BaseResponse").asJSONObject();
			if (null != BaseResponse) {
				int ret = BaseResponse.getInt("Ret", -1);
				if (ret == 0) {
					JSONArray memberList = jsonObject.get("MemberList").asArray();
					if (null != memberList) {
						addToContact(memberList, wechatMeta);
					}
				}
			}
			this.getGroup(wechatMeta);
		} catch (Exception e) {
			LOGGER.error("解析联系人报文失败", e);
			throw new WechatException(e);
		}
	}

	private void getGroup(WechatMeta wechatMeta) {
		String url = wechatMeta.getBase_uri() + "/webwxbatchgetcontact?type=ex&pass_ticket="
				+ wechatMeta.getPass_ticket() + "&skey=" + wechatMeta.getSkey() + "&r=" + DateKit.getCurrentUnixTime();

		JSONObject body = new JSONObject();
		body.put("BaseRequest", wechatMeta.getBaseRequest());

		HttpRequest request = HttpRequest.post(url).contentType("application/json;charset=utf-8")
				.header("Cookie", wechatMeta.getCookie()).send(body.toString());

		LOGGER.debug(request.toString());
		String res = request.body();
		request.disconnect();

		if (StringKit.isBlank(res)) {
			LOGGER.error("获取群信息失败");
			throw new WechatException("获取群信息失败");
		}

		LOGGER.debug(res);

		try {
			JSONObject jsonObject = JSONKit.parseObject(res);
			JSONObject BaseResponse = jsonObject.get("BaseResponse").asJSONObject();
			if (null != BaseResponse) {
				int ret = BaseResponse.getInt("Ret", -1);
				if (ret == 0) {
					JSONArray memberList = jsonObject.get("ContactList").asArray();
					if (null != memberList) {
						addToContact(memberList, wechatMeta);
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("获取群信息失败", e);
			throw new WechatException(e);
		}
	}

	/**
	 * 获取UUID
	 */
	@Override
	public String getUUID() throws WechatException {
		HttpRequest request = HttpRequest.get(Constant.JS_LOGIN_URL, true, "appid", "wx782c26e4c19acffb", "fun", "new",
				"lang", "zh_CN", "_", DateKit.getCurrentUnixTime());

		LOGGER.debug(request.toString());

		String res = request.body();
		request.disconnect();

		if (StringKit.isNotBlank(res)) {
			String code = Matchers.match("window.QRLogin.code = (\\d+);", res);
			if (null != code) {
				if (code.equals("200")) {
					return Matchers.match("window.QRLogin.uuid = \"(.*)\";", res);
				} else {
					LOGGER.error("错误的状态码: " + code);
					throw new WechatException("错误的状态码: " + code);
				}
			}
		}
		LOGGER.error("获取UUID失败");
		throw new WechatException("获取UUID失败");
	}

	/**
	 * 打开状态提醒
	 */
	@Override
	public void openStatusNotify(WechatMeta wechatMeta) throws WechatException {

		String url = wechatMeta.getBase_uri() + "/webwxstatusnotify?lang=zh_CN&pass_ticket="
				+ wechatMeta.getPass_ticket();

		JSONObject body = new JSONObject();
		body.put("BaseRequest", wechatMeta.getBaseRequest());
		body.put("Code", 3);
		body.put("FromUserName", wechatMeta.getUser().getString("UserName"));
		body.put("ToUserName", wechatMeta.getUser().getString("UserName"));
		body.put("ClientMsgId", DateKit.getCurrentUnixTime());

		HttpRequest request = HttpRequest.post(url).contentType("application/json;charset=utf-8")
				.header("Cookie", wechatMeta.getCookie()).send(body.toString());

		LOGGER.debug("" + request);
		String res = request.body();
		request.disconnect();

		if (StringKit.isBlank(res)) {
			LOGGER.error("状态通知开启失败");
			throw new WechatException("状态通知开启失败");
		}

		try {
			JSONObject jsonObject = JSONKit.parseObject(res);
			JSONObject BaseResponse = jsonObject.get("BaseResponse").asJSONObject();
			if (null != BaseResponse) {
				int ret = BaseResponse.getInt("Ret", -1);
				if (ret != 0) {
					LOGGER.error("状态通知开启失败，ret：" + ret);
					throw new WechatException("状态通知开启失败，ret：" + ret);
				}
			}
		} catch (Exception e) {
			LOGGER.error("状态通知开启失败：", e);
			throw new WechatException(e);
		}
	}

	/**
	 * 微信初始化
	 */
	@Override
	public void wxInit(WechatMeta wechatMeta) throws WechatException {
		String url = wechatMeta.getBase_uri() + "/webwxinit?r=" + DateKit.getCurrentUnixTime() + "&pass_ticket="
				+ wechatMeta.getPass_ticket() + "&skey=" + wechatMeta.getSkey();

		JSONObject body = new JSONObject();
		body.put("BaseRequest", wechatMeta.getBaseRequest());

		HttpRequest request = HttpRequest.post(url).contentType("application/json;charset=utf-8")
				.header("Cookie", wechatMeta.getCookie()).send(body.toString());

		LOGGER.debug("" + request);
		String res = request.body();
		request.disconnect();

		if (StringKit.isBlank(res)) {
			LOGGER.error("微信初始化失败");
			throw new WechatException("微信初始化失败");
		}

		try {
			JSONObject jsonObject = JSONKit.parseObject(res);
			if (null != jsonObject) {
				JSONObject BaseResponse = jsonObject.get("BaseResponse").asJSONObject();
				if (null != BaseResponse) {
					int ret = BaseResponse.getInt("Ret", -1);
					if (ret == 0) {
						wechatMeta.setSyncKey(jsonObject.get("SyncKey").asJSONObject());
						wechatMeta.setUser(jsonObject.get("User").asJSONObject());

						StringBuffer synckey = new StringBuffer();
						JSONArray list = wechatMeta.getSyncKey().get("List").asArray();
						for (int i = 0, len = list.size(); i < len; i++) {
							JSONObject item = list.get(i).asJSONObject();
							synckey.append("|" + item.getInt("Key", 0) + "_" + item.getInt("Val", 0));
						}
						wechatMeta.setSynckey(synckey.substring(1));
						//
						JSONArray contactList = jsonObject.get("ContactList").asArray();
						if (contactList != null) {
							addToContact(contactList, wechatMeta);
						}
						//

					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("微信初始化失败", e);
			throw new WechatException("微信初始化失败");
		}
	}

	/**
	 * 
	 * @param contactList
	 * @param wechatMeta
	 */
	private void addToContact(JSONArray contactList, WechatMeta wechatMeta) {

		if (Constant.CONTACT == null) {
			Constant.CONTACT = new WechatContact();
		}
		if (Constant.CONTACT.getGroupList() == null) {
			Constant.CONTACT.setGroupList(new JSONArray());
		}
		if (Constant.CONTACT.getMemberList() == null) {
			Constant.CONTACT.setMemberList(new JSONArray());
		}

		for (int i = 0, len = contactList.size(); i < len; i++) {
			JSONObject contact = contactList.get(i).asJSONObject();
			LOGGER.info(contact.getString("UserName") + "/" + contact.getString("NickName"));
			//
			// 公众号/服务号
			if (contact.getInt("VerifyFlag", 0) == 8) {
				continue;
			}
			// 特殊联系人
			if (Constant.FILTER_USERS.contains(contact.getString("UserName"))) {
				continue;
			}
			// 群聊
			if (contact.getString("UserName").indexOf("@@") != -1) {
				if (!groupExists(contact)) {
					LOGGER.info("Add group:" + contact.getString("NickName"));
					Constant.CONTACT.getGroupList().add(contact);
				}
				continue;
			}
			// 自己
			if (contact.getString("UserName").equals(wechatMeta.getUser().getString("UserName"))) {
				continue;
			}
			// 联系人
			if (!memberExists(contact)) {
				LOGGER.info("Add contact:" + contact.getString("NickName"));
				Constant.CONTACT.getMemberList().add(contact);

			}
		}
	}

	/**
	 * 选择同步线路
	 */
	@Override
	public void choiceSyncLine(WechatMeta wechatMeta) throws WechatException {
		boolean enabled = false;
		for (String syncUrl : Constant.SYNC_HOST) {
			int[] res = this.syncCheck(syncUrl, wechatMeta);
			if (res[0] == 0) {
				String url = "https://" + syncUrl + "/cgi-bin/mmwebwx-bin";
				wechatMeta.setWebpush_url(url);
				LOGGER.info("选择线路：[{}]", syncUrl);
				enabled = true;
				break;
			}
		}
		if (!enabled) {
			LOGGER.error("同步线路不通畅");
			throw new WechatException("同步线路不通畅");
		}
	}

	/**
	 * 检测心跳
	 */
	@Override
	public int[] syncCheck(WechatMeta wechatMeta) throws WechatException {
		return this.syncCheck(null, wechatMeta);
	}

	/**
	 * 检测心跳
	 */
	private int[] syncCheck(String url, WechatMeta meta) {

		// 如果网络中断，休息10秒
		if (PingUtil.netIsOver()) {
			try {
				TimeUnit.SECONDS.sleep(10);
			} catch (Exception e) {
				LOGGER.error("", e);
			}
		}

		if (null == url) {
			url = meta.getWebpush_url() + "/synccheck";
		} else {
			url = "https://" + url + "/cgi-bin/mmwebwx-bin/synccheck";
		}

		JSONObject body = new JSONObject();
		body.put("BaseRequest", meta.getBaseRequest());

		HttpRequest request = HttpRequest
				.get(url, true, "r", DateKit.getCurrentUnixTime() + StringKit.getRandomNumber(5), "skey",
						meta.getSkey(), "uin", meta.getWxuin(), "sid", meta.getWxsid(), "deviceid", meta.getDeviceId(),
						"synckey", meta.getSynckey(), "_", System.currentTimeMillis())
				.header("Cookie", meta.getCookie());

		LOGGER.debug(request.toString());
		String res = null;
		try {
			res = request.body();
			request.disconnect();
		} catch (Exception e) {
			LOGGER.error("心跳异常", e);
			e.printStackTrace();
		}
		int[] arr = new int[] { -1, -1 };
		if (StringKit.isBlank(res)) {
			return arr;
		}

		String retcode = Matchers.match("retcode:\"(\\d+)\",", res);
		String selector = Matchers.match("selector:\"(\\d+)\"}", res);
		if (null != retcode && null != selector) {
			arr[0] = Integer.parseInt(retcode);
			arr[1] = Integer.parseInt(selector);
			return arr;
		}
		return arr;
	}

	/**
	 * 处理消息
	 */
	@Override
	public void handleMsg(WechatMeta wechatMeta, JSONObject data) {
		if (null == data) {
			return;
		}

		JSONArray AddMsgList = data.get("AddMsgList").asArray();

		for (int i = 0, len = AddMsgList.size(); i < len; i++) {
			LOGGER.info("你有新的消息，请注意查收");
			JSONObject msg = AddMsgList.get(i).asJSONObject();
			int msgType = msg.getInt("MsgType", 0);
			String name = getUserRemarkName(msg.getString("FromUserName"));
			String content = msg.getString("Content");

			if (msgType == 51) {
				LOGGER.info("成功截获微信初始化消息");
			} else if (msgType == 1) {
				if (Constant.FILTER_USERS.contains(msg.getString("ToUserName"))) {
					continue;
				} else if (msg.getString("FromUserName").equals(wechatMeta.getUser().getString("UserName"))) {
					// 发送的消息
					LOGGER.info("发送的消息：" + content);
					continue;
				} else if (msg.getString("ToUserName").indexOf("@@") != -1) {
					String[] peopleContent = content.split(":<br/>");
					LOGGER.info("|" + name + "| " + peopleContent[0] + ":\n" + peopleContent[1].replace("<br/>", "\n"));
				} else {
					LOGGER.info(name + ": " + content);
					// String ans = robot.talk(content);
					// webwxsendmsg(wechatMeta, ans,
					// msg.getString("FromUserName"));
					// LOGGER.info("自动回复 " + ans);
				}
			} else if (msgType == 3) {
				String imgDir = Constant.config.get("app.img_path");
				String msgId = msg.getString("MsgId");
				FileKit.createDir(imgDir, false);
				String imgUrl = wechatMeta.getBase_uri() + "/webwxgetmsgimg?MsgID=" + msgId + "&skey="
						+ wechatMeta.getSkey() + "&type=slave";
				HttpRequest.get(imgUrl).header("Cookie", wechatMeta.getCookie())
						.receive(new File(imgDir + "/" + msgId + ".jpg"));
				// webwxsendmsg(wechatMeta, "二蛋还不支持图片呢",
				// msg.getString("FromUserName"));
			} else if (msgType == 34) {
				// webwxsendmsg(wechatMeta, "二蛋还不支持语音呢",
				// msg.getString("FromUserName"));
			} else if (msgType == 42) {
				LOGGER.info(name + " 给你发送了一张名片:");
				LOGGER.info("=========================");
			}
		}
	}

	/**
	 * 发送消息
	 */
	public void webwxsendmsg(WechatMeta meta, String content, String to) throws WechatException {
		String url = meta.getBase_uri() + "/webwxsendmsg?lang=zh_CN&pass_ticket=" + meta.getPass_ticket();
		JSONObject body = new JSONObject();

		String clientMsgId = DateKit.getCurrentUnixTime() + StringKit.getRandomNumber(5);
		JSONObject Msg = new JSONObject();
		Msg.put("Type", 1);
		Msg.put("Content", content);
		Msg.put("FromUserName", meta.getUser().getString("UserName"));
		Msg.put("ToUserName", to);
		Msg.put("LocalID", clientMsgId);
		Msg.put("ClientMsgId", clientMsgId);

		body.put("BaseRequest", meta.getBaseRequest());
		body.put("Msg", Msg);

		HttpRequest request = HttpRequest.post(url).connectTimeout(30000).readTimeout(30000)
				.contentType("application/json;charset=utf-8").header("Cookie", meta.getCookie()).send(body.toString());

		LOGGER.info("发送消息...");
		LOGGER.debug("" + request);
		String res = request.body();
		request.disconnect();
		if (StringKit.isBlank(res)) {
			LOGGER.error("发送消息失败");
			throw new WechatException("发送消息失败");
		}
	}

	/**
	 * 发送图片
	 */
	public void webwxsendimg(WechatMeta meta, byte[] imgData, String to) throws WechatException {
		String url = Constant.FILE_URL + "?f=json";

		JSONObject info = new JSONObject();
		String clientMsgId = DateKit.getCurrentUnixTime() + StringKit.getRandomNumber(5);
		// "{\"UploadType\":2,\"BaseRequest\":{\"Uin\":" + WeixinSendMain.UIN +
		// ",\"Sid\":\""
		// + SID + "\",\"Skey\":\"" + KEY
		// +
		// "\",\"DeviceID\":\"@deviceId@\"},\"ClientMediaId\":@mediaId@,\"TotalLen\":@dataLen@,\"StartPos\":0,\"DataLen\":@dataLen@,\"MediaType\":4,\"FromUserName\":\""
		// + FROM_USER + "\",\"ToUserName\":\"" + TO_USER +
		// "\",\"FileMd5\":\"@md5@\"}";
		info.put("UploadType", 2);
		info.put("BaseRequest", meta.getBaseRequest());
		info.put("ClientMediaId", clientMsgId);
		info.put("TotalLen", imgData.length);
		info.put("StartPos", 0);
		info.put("DataLen", imgData.length);
		info.put("MediaType", 4);
		info.put("FromUserName", meta.getUser().getString("UserName"));
		info.put("ToUserName", to);
		info.put("FileMd5", Md5Util.getMd5(imgData));

		int count = ai.getAndIncrement();
		HttpRequest request = HttpRequest.post(url).connectTimeout(30000).readTimeout(30000)
				.header("Cookie", meta.getCookie()).part("id", "WU_FILE_" + (count % 10))
				.part("name", "untitled" + count + ".jpg").part("type", "image/jpeg")
				.part("lastModifiedDate", String.valueOf(System.currentTimeMillis()))
				.part("size", String.valueOf(imgData.length)).part("mediatype", "pic")
				.part("uploadmediarequest", info.toString()).part("webwx_data_ticket", meta.getPass_ticket())
				.part("pass_ticket", "undefined").part("filename", "untitled" + count + ".jpg",
						"application/octet-stream", new ByteArrayInputStream(imgData));

		LOGGER.info("发送图片消息...");
		LOGGER.debug("" + request);
		String resp = request.body();
		request.disconnect();

		if (StringKit.isBlank(resp)) {
			throw new WechatException("发送图片信息失败");
		}

		LOGGER.debug(resp);
		//
		if (resp != null) {
			JSONObject respObj = JSONKit.parseObject(resp);
			String mediaId = respObj.getString("MediaId");
			String localId = DateKit.getCurrentUnixTime() + StringKit.getRandomNumber(5);
			JSONObject body = new JSONObject();
			body.put("BaseRequest", meta.getBaseRequest());
			JSONObject msg = new JSONObject();
			msg.put("Type", 3);
			msg.put("MediaId", mediaId);
			msg.put("Content", "");
			msg.put("FromUserName", meta.getUser().getString("UserName"));
			msg.put("ToUserName", to);
			msg.put("LocalID", localId);
			msg.put("ClientMsgId", localId);
			body.put("Msg", msg);
			body.put("Scene", 0);
			url = meta.getBase_uri() + "/webwxsendmsgimg?fun=async&f=json";
			HttpRequest request2 = HttpRequest.post(url).connectTimeout(30000).readTimeout(30000)
					.contentType("application/json;charset=utf-8").header("Cookie", meta.getCookie())
					.send(body.toString());
			LOGGER.debug("" + request2);
			String resp2 = request2.body();
			request2.disconnect();
			if (StringKit.isBlank(resp2)) {
				throw new WechatException("发送图片信息失败");
			}
			LOGGER.debug(resp2);
		} else {
			LOGGER.error("图片提交失败。");
		}
	}

	private String getUserRemarkName(String id) {
		String name = "这个人物名字未知";
		for (int i = 0, len = Constant.CONTACT.getMemberList().size(); i < len; i++) {
			JSONObject member = Constant.CONTACT.getMemberList().get(i).asJSONObject();
			if (member.getString("UserName").equals(id)) {
				if (StringKit.isNotBlank(member.getString("RemarkName"))) {
					name = member.getString("RemarkName");
				} else {
					name = member.getString("NickName");
				}
				return name;
			}
		}
		return name;
	}

	@Override
	public JSONObject webwxsync(WechatMeta meta) throws WechatException {

		String url = meta.getBase_uri() + "/webwxsync?skey=" + meta.getSkey() + "&sid=" + meta.getWxsid();

		JSONObject body = new JSONObject();
		body.put("BaseRequest", meta.getBaseRequest());
		body.put("SyncKey", meta.getSyncKey());
		body.put("rr", DateKit.getCurrentUnixTime());

		HttpRequest request = HttpRequest.post(url).contentType("application/json;charset=utf-8")
				.header("Cookie", meta.getCookie()).send(body.toString());

		LOGGER.debug(request.toString());
		String res = request.body();
		request.disconnect();

		if (StringKit.isBlank(res)) {
			throw new WechatException("同步syncKey失败");
		}

		JSONObject jsonObject = JSONKit.parseObject(res);
		JSONObject BaseResponse = jsonObject.get("BaseResponse").asJSONObject();
		if (null != BaseResponse) {
			int ret = BaseResponse.getInt("Ret", -1);
			if (ret == 0) {

				JSONArray groupList = jsonObject.get("ModContactList").asArray();
				if (groupList != null && groupList.size() > 0) {
					addToContact(groupList, meta);
				}
				meta.setSyncKey(jsonObject.get("SyncKey").asJSONObject());
				StringBuffer synckey = new StringBuffer();
				JSONArray list = meta.getSyncKey().get("List").asArray();
				for (int i = 0, len = list.size(); i < len; i++) {
					JSONObject item = list.get(i).asJSONObject();
					synckey.append("|" + item.getInt("Key", 0) + "_" + item.getInt("Val", 0));
				}
				meta.setSynckey(synckey.substring(1));
				return jsonObject;
			}
		}
		return null;
	}

	private boolean groupExists(JSONObject contact) {
		if (Constant.CONTACT.getGroupList() == null)
			return false;
		for (int i = 0, len = Constant.CONTACT.getGroupList().size(); i < len; i++) {
			JSONObject e = Constant.CONTACT.getGroupList().get(i).asJSONObject();
			if (e.getString("UserName").equals(contact.getString("UserName"))) {
				return true;
			}
		}
		return false;
	}

	private boolean memberExists(JSONObject contact) {
		if (Constant.CONTACT.getMemberList() == null)
			return false;
		for (int i = 0, len = Constant.CONTACT.getMemberList().size(); i < len; i++) {
			JSONObject e = Constant.CONTACT.getMemberList().get(i).asJSONObject();
			if (e.getString("UserName").equals(contact.getString("UserName"))) {
				return true;
			}
		}
		return false;
	}

}
