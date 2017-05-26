package me.biezhi.wechat.service;

import com.blade.kit.json.JSONObject;

import me.biezhi.wechat.exception.WechatException;
import me.biezhi.wechat.model.WechatMeta;

public interface WechatService {

	/**
	 * 获取UUID
	 * 
	 * @return
	 */
	String getUUID() throws WechatException;

	/**
	 * 微信初始化
	 * 
	 * @param wechatMeta
	 * @throws WechatException
	 */
	void wxInit(WechatMeta wechatMeta) throws WechatException;

	/**
	 * 开启状态通知
	 * 
	 * @return
	 */
	void openStatusNotify(WechatMeta wechatMeta) throws WechatException;

	/**
	 * 获取联系人
	 * 
	 * @param wechatMeta
	 * @return
	 */
	void getContact(WechatMeta wechatMeta) throws WechatException;

	/**
	 * 选择同步线路
	 * 
	 * @param wechatMeta
	 * @return
	 * @throws WechatException
	 */
	void choiceSyncLine(WechatMeta wechatMeta) throws WechatException;

	/**
	 * 消息检查
	 * 
	 * @param wechatMeta
	 * @return
	 */
	int[] syncCheck(WechatMeta wechatMeta) throws WechatException;

	/**
	 * 处理聊天信息
	 * 
	 * @param wechatMeta
	 * @param data
	 */
	void handleMsg(WechatMeta wechatMeta, JSONObject data);

	/**
	 * 获取最新消息
	 * 
	 * @param meta
	 * @return
	 */
	JSONObject webwxsync(WechatMeta meta) throws WechatException;

	/**
	 * 发送文字消息
	 * 
	 * @param meta
	 * @param content
	 * @param to
	 */
	void webwxsendmsg(WechatMeta meta, String content, String to) throws WechatException;

	/**
	 * 发送图片消息
	 * 
	 * @param meta
	 * @param imgData
	 * @param to
	 */
	void webwxsendimg(WechatMeta meta, byte[] imgData, String to) throws WechatException;
}
