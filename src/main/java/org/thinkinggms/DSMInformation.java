package org.thinkinggms;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import org.thinkinggms.utils.FileUtils;
import org.thinkinggms.utils.SQLUtils;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class DSMInformation {
    public static JDA jda;
    public static Random random = ThreadLocalRandom.current();

    public static void main(String[] args) {
        SQLUtils.initializeConnection();
        JDABuilder builder = JDABuilder.createDefault(FileUtils.secretResources.get("bot_token").getAsString());
        builder.setAutoReconnect(true);
        builder.setMemberCachePolicy(MemberCachePolicy.ALL);
        builder.setStatus(OnlineStatus.ONLINE);
        builder.setActivity(Activity.listening("명령어 사용 정보"));
        builder.addEventListeners(new DiscordListener());
        builder.enableIntents(
                GatewayIntent.MESSAGE_CONTENT,
                GatewayIntent.GUILD_MEMBERS,
                GatewayIntent.GUILD_PRESENCES
        );
        (jda = builder.build()).updateCommands().addCommands(
                new CommandDataImpl("급식표", "특정 날짜, 특정 시간대의 급식을 가져옵니다.").addOptions(
                        new OptionData(OptionType.STRING, "날짜", "급식을 가져올 날짜입니다. (YYYYMMDD 형식으로 작성)").setRequired(false),
                        new OptionData(OptionType.STRING, "조중석", "조식/중식/석식의 급식을 따로 가져올 수 있습니다").setRequired(false)
                                .addChoice("조식", "조식")
                                .addChoice("중식", "중식")
                                .addChoice("석식", "석식")
                ), new CommandDataImpl("시간표", "한 주의 시간표를 출력합니다.").addOptions(
                        new OptionData(OptionType.STRING, "학반", "기준 학년과 반입니다. (ex. 1-1)").setRequired(false)
                                .addChoice("1-1", "1-1")
                                .addChoice("1-2", "1-2")
                                .addChoice("1-3", "1-3")
                                .addChoice("1-4", "1-4")
                                .addChoice("2-1", "2-1")
                                .addChoice("2-2", "2-2")
                                .addChoice("2-3", "2-3")
                                .addChoice("2-4", "2-4")
                                .addChoice("3-1", "3-1")
                                .addChoice("3-2", "3-2")
                                .addChoice("3-3", "3-3")
                                .addChoice("3-4", "3-4")
                ), new CommandDataImpl("일정추가", "개인 일정을 추가합니다.").addOptions(
                        new OptionData(OptionType.STRING, "날짜", "일정을 추가할 날짜입니다. (YYYY-MM-DD 형식으로 작성)").setRequired(true),
                        new OptionData(OptionType.STRING, "시간", "정확한 시간을 추가할 수 있습니다. (HH:MM:SS 형식으로 작성)").setRequired(false),
                        new OptionData(OptionType.STRING, "설명", "일정에 설명을 추가할 수 있습니다.").setRequired(false)
                ), new CommandDataImpl("일정제거", "아이디로 일정을 제거합니다.").addOptions(
                        new OptionData(OptionType.INTEGER, "아이디", "일정의 아이디를 입력해주세요.").setRequired(true)
                                .setMinValue(0)
                ), new CommandDataImpl("일정확인", "현재 추가한 일정을 확인할 수 있습니다.").addOptions(
                        new OptionData(OptionType.USER, "유저", "확인하고싶은 유저를 정할 수 있습니다.").setRequired(false)
                ), new CommandDataImpl("다음학사일정", "제일 최근에 위치한 학사 일정을 확인합니다.").addOptions(
                        new OptionData(OptionType.INTEGER, "학년", "특정 학년의 학사일정만 볼 수 있습니다.").setRequired(false)
                                .setMinValue(1)
                                .setMaxValue(3)
                ), new CommandDataImpl("다음쉬는날", "다음 쉬는날을 확인합니다.").addOptions(
                        new OptionData(OptionType.BOOLEAN, "토요일제외", "쉬는 날 중 토요일을 제외합니다. (기본값: True)").setRequired(false)
                ), new CommandDataImpl("학습", "루아에게 메시지를 학습시킵니다.").addOptions(
                        new OptionData(OptionType.STRING, "입력메시지", "반응할 메시지를 정합니다. (ex. 대마고)").setRequired(true).setMinLength(2).setMaxLength(100),
                        new OptionData(OptionType.STRING, "출력메시지", "대답할 메시지를 정합니다. (ex. 대덕소프트웨어마이스터고등학교)").setRequired(true).setMinLength(2).setMaxLength(100)
                ), new CommandDataImpl("학습확인", "루아에게 학습시킨 메시지를 확인합니다.").addOptions(
                        new OptionData(OptionType.USER, "유저", "확인할 유저를 정합니다.").setRequired(false)
                ), new CommandDataImpl("망각", "루아에게 가르친 메시지를 망각시킵니다.").addOptions(
                        new OptionData(OptionType.STRING, "입력메시지", "입력 메시지를 입력합니다.").setRequired(true),
                        new OptionData(OptionType.STRING, "출력메시지", "출력 메시지를 입력합니다.").setRequired(false)
                ), new CommandDataImpl("방학까지", "다음 방학까지 남은 시간을 표시합니다.")).queue();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            jda.shutdown();
            SQLUtils.closeConnection();
        }));
    }
}
