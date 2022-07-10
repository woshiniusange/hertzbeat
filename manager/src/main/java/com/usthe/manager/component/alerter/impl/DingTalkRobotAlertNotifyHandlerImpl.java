package com.usthe.manager.component.alerter.impl;

import com.usthe.alert.AlerterProperties;
import com.usthe.common.entity.alerter.Alert;
import com.usthe.common.entity.manager.NoticeReceiver;
import com.usthe.common.util.CommonConstants;
import com.usthe.common.util.CommonUtil;
import com.usthe.common.util.ResourceBundleUtil;
import com.usthe.manager.component.alerter.AlertNotifyHandler;
import com.usthe.manager.pojo.dto.DingTalkWebHookDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ResourceBundle;

/**
 * Send alarm information through DingTalk robot
 * 通过钉钉机器人发送告警信息
 *
 * @author <a href="mailto:Musk.Chen@fanruan.com">Musk.Chen</a>
 * @since 2022/4/24
 */
@Component
@RequiredArgsConstructor
@Slf4j
final class DingTalkRobotAlertNotifyHandlerImpl implements AlertNotifyHandler {

    private final RestTemplate restTemplate;

    private final AlerterProperties alerterProperties;

    private ResourceBundle bundle = ResourceBundleUtil.getBundle("alerter");

    @Override
    public void send(NoticeReceiver receiver, Alert alert) {
        String monitorId = null;
        String monitorName = null;
        if (alert.getTags() != null) {
            monitorId = alert.getTags().get(CommonConstants.TAG_MONITOR_ID);
            monitorName = alert.getTags().get(CommonConstants.TAG_MONITOR_NAME);
        }
        DingTalkWebHookDto dingTalkWebHookDto = new DingTalkWebHookDto();
        DingTalkWebHookDto.MarkdownDTO markdownDTO = new DingTalkWebHookDto.MarkdownDTO();
        StringBuilder contentBuilder = new StringBuilder("#### [" + bundle.getString("alerter.notify.title")
                + "]\n##### **" + bundle.getString("alerter.notify.target") + "** : " +
                alert.getTarget() + "\n   ");
        if (monitorId != null) {
            contentBuilder.append("##### **").append(bundle.getString("alerter.notify.monitorId"))
                    .append("** : ").append(monitorId).append("\n   ");
        }
        if (monitorName != null) {
            contentBuilder.append("##### **").append(bundle.getString("alerter.notify.monitorName"))
                    .append("** : ").append(monitorName).append("\n   ");
        }
        contentBuilder.append("##### **").append(bundle.getString("alerter.notify.priority"))
                .append("** : ").append(bundle.getString("alerter.priority." + alert.getPriority())).append("\n   ");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String triggerTime = simpleDateFormat.format(new Date(alert.getLastTriggerTime()));
        contentBuilder.append("##### **").append(bundle.getString("alerter.notify.triggerTime"))
                .append("** : ").append(triggerTime).append("\n   ");
        contentBuilder.append("##### **").append(bundle.getString("alerter.notify.content"))
                .append("** : ").append(alert.getContent());
        markdownDTO.setText(contentBuilder.toString());
        markdownDTO.setTitle(bundle.getString("alerter.notify.title"));
        dingTalkWebHookDto.setMarkdown(markdownDTO);
        String webHookUrl = alerterProperties.getDingTalkWebHookUrl() + receiver.getAccessToken();
        try {
            ResponseEntity<String> entity = restTemplate.postForEntity(webHookUrl, dingTalkWebHookDto, String.class);
            if (entity.getStatusCode() == HttpStatus.OK) {
                log.debug("Send dingTalk webHook: {} Success", webHookUrl);
            } else {
                log.warn("Send dingTalk webHook: {} Failed: {}", webHookUrl, entity.getBody());
            }
        } catch (ResourceAccessException e) {
            log.warn("Send dingTalk: {} Failed: {}.", webHookUrl, e.getMessage());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public byte type() {
        return 5;
    }
}
