package org.thinkinggms;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.FileUpload;
import org.jetbrains.annotations.NotNull;
import org.thinkinggms.utils.ImageUtils;
import org.thinkinggms.utils.SQLUtils;
import org.thinkinggms.utils.StringUtils;
import org.thinkinggms.utils.URLUtils;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class DiscordListener implements EventListener {

	@SuppressWarnings("deprecation")
	private static @NotNull String format(@NotNull Date date, String div) {
		return leftPad(String.valueOf(date.getYear() + 1900), 4) + div + leftPad(String.valueOf(date.getMonth() + 1), 2) + div + leftPad(String.valueOf(date.getDate()), 2);
	}

	private static @NotNull String leftPad(@NotNull String s, int n) {
		if (s.length() >= n) return s;
		else return "0".repeat(n - s.length()) + s;
	}

	public static @NotNull String formatStr(@NotNull String date) {
		return date.length() > 6 ? date.substring(0, 4) + "년 " + date.substring(4, 6) + "월 " + date.substring(6) + "일" : "20" + date.substring(0, 2) + "년 " + date.substring(2, 4) + "월 " + date.substring(4) + "일";
	}

	public static @NotNull String tone(String date) {
		if (format(new Date(), "").equalsIgnoreCase(date) || format(new Date(), "").equalsIgnoreCase("20" + date)) return "(오늘)";
		if (format(new Date(new Date().getTime() + 86400000), "").equalsIgnoreCase(date) || format(new Date(new Date().getTime() + 86400000), "").equalsIgnoreCase("20" + date)) return "(내일)";
		return "";
	}

	@SuppressWarnings("SpellCheckingInspection")
    @Override
	public void onEvent(@NotNull GenericEvent e) {
		if (e instanceof SlashCommandInteractionEvent event) {
			switch (event.getName()) {
				case "급식표" -> {
					label:
					{
						String dateString = format(new Date(), "");
						var date = event.getOption("날짜");
						if (date != null) dateString = date.getAsString().equalsIgnoreCase("내일") ? format(new Date(new Date().getTime() + 86400000), "") : date.getAsString();
						JsonObject mealInfo = URLUtils.getMealInfo(dateString);
						var ddm = event.getOption("조중석");
						if (mealInfo.getAsJsonArray("mealServiceDietInfo") == null) break label;
						JsonArray arr = mealInfo.getAsJsonArray("mealServiceDietInfo").get(1).getAsJsonObject().getAsJsonArray("row");
						var action = event.reply("# " + formatStr(dateString) + tone(dateString) + "의 급식 정보" + (ddm != null ? " (" + (ddm.getAsString().length() == 1 ? ddm.getAsString() + "식" : ddm.getAsString()) + ")" : ""));
						boolean check = true;
						for (JsonElement el : arr) {
							JsonObject o = el.getAsJsonObject();
							String s = o.get("MMEAL_SC_NM").getAsString() + " (" + o.get("CAL_INFO").getAsString() + ")";
							if (ddm != null) {
								if (s.startsWith(ddm.getAsString())) {
									check = false;
									action.addEmbeds(new EmbedBuilder().setTitle(s).setDescription(o.get("DDISH_NM").getAsString().replace("<br/>", "\n").replaceAll("\\((\\d+\\.?)+\\)", "")).build());
								}
							} else {
								check = false;
								action.addEmbeds(new EmbedBuilder().setTitle(s).setDescription(o.get("DDISH_NM").getAsString().replace("<br/>", "\n").replaceAll("\\((\\d+\\.?)+\\)", "")).build());
							}
						}
						if (check) break label;
						action.queue();
						return;
					}
					event.reply("이날은 급식이 없어.").queue();
				}
				case "시간표" -> {
					OptionMapping gc = event.getOption("학반");
					int[] gci;
					String addition = "";
					if (gc == null || !gc.getAsString().matches("^[123]-[1234]$")) {
						gci = SQLUtils.getGCI(event.getUser().getId());
						if (gci[0] == 0) {
							event.reply("학년이랑 반이 있어야 시간표를 알려주지..").queue();
							return;
						}
					} else {
						gci = Arrays.stream(gc.getAsString().split("-")).mapToInt(Integer::parseInt).toArray();
						if (SQLUtils.modifyGCI(event.getUser().getId(), gci[0], gci[1])) addition = "다음부터 학반은 " + gci[0] + "-" + gci[1] + "로 맞춰줄게.";
					}
					JsonArray arr = URLUtils.parseTimeTable(URLUtils.baseTimeTable(), gci[0], gci[1]);
					BufferedImage image = ImageUtils.renderTimeTable(arr, gci[0], gci[1]);
					InputStream is = ImageUtils.bufferedImageToInputStream(image);
					if (is == null) {
						event.reply("내 시간표가 물에 젖었어. 나중에 다시 시도해줘.").queue();
						return;
					}
					event.replyFiles(FileUpload.fromData(is, gci[0] + "-" + gci[1] + "-timeTable.png")).addContent(addition).queue();
				}
				case "일정추가" -> {
					OptionMapping date = event.getOption("날짜");
					OptionMapping time = event.getOption("시간");
					OptionMapping description = event.getOption("설명");
					int id = SQLUtils.addEvent(date != null ? date.getAsString() : format(new Date(), "-"), time == null ? "00:00:00" : time.getAsString(), description != null ? description.getAsString() : "", event.getUser().getId());
					if (id != -1) event.reply(id + "번 일정.. 추가 완료").queue();
					else event.reply("노트가 가득 찼어.").queue();
				}
				case "일정제거" -> {
					OptionMapping id = event.getOption("아이디");
					if (SQLUtils.removeEvent(id != null ? id.getAsInt() : -1, event.getUser().getId())) event.reply("알겠어, 지금 지워줄게.").queue();
					else event.reply("노트에 일정이 많아서 그런 걸수도 있는데, 그런 일정은 없어.").queue();
				}
				case "일정확인" -> {
					OptionMapping user = event.getOption("유저");
					User u = event.getUser();
					if (user != null) u = user.getAsUser();
					var action = event.reply("");
					SQLUtils.getEvents(u.getId(), action);
					action.queue();
				}
				case "다음학사일정" -> {
					OptionMapping grade = event.getOption("학년");
					try {
						JsonObject data = URLUtils.getSchoolSchedule(format(new Date(), ""), 100);
						JsonObject o;
						if (grade != null) {
							String gString = switch (grade.getAsInt()) {
                                case 1 -> "ONE";
                                case 2 -> "TW";
                                case 3 -> "THREE";
                                default -> "";
                            };
                            o = data.getAsJsonArray("SchoolSchedule").get(1).getAsJsonObject().getAsJsonArray("row").asList().stream().filter(el -> el.getAsJsonObject().get(gString + "_GRADE_EVENT_YN").getAsString().equalsIgnoreCase("Y") && !el.getAsJsonObject().get("EVENT_NM").getAsString().equalsIgnoreCase("토요휴업일")).limit(1).toList().getFirst().getAsJsonObject();
						} else {
							o = data.getAsJsonArray("SchoolSchedule").get(1).getAsJsonObject().getAsJsonArray("row").asList().stream().filter(el -> !el.getAsJsonObject().get("EVENT_NM").getAsString().equalsIgnoreCase("토요휴업일")).toList().getFirst().getAsJsonObject();
						}
						String dateString = o.get("AA_YMD").getAsString();
						event.reply("다음 " + (grade != null ? grade.getAsInt() + "학년 " : "") + "학사 일정은 **\"" + o.get("EVENT_NM").getAsString() + "\"**(이)야.\n" + dateString.substring(0, 4) + "년 " + dateString.substring(4, 6) + "월 " + dateString.substring(6) + "일이니까 까먹지 말라고.").queue();
					} catch (Throwable t) {
						t.printStackTrace(System.out);
						event.reply("아직 올라온 학사 일정이 없는 것 같네, 다음에 와.").setEphemeral(true).queue();
					}
				}
				case "방학까지" -> {
					try {
						JsonObject data = URLUtils.getSchoolSchedule(format(new Date(), ""), 1000);
						int date = Integer.parseInt(format(new Date(), ""));
						JsonObject o = data.getAsJsonArray("SchoolSchedule").get(1).getAsJsonObject().getAsJsonArray("row").asList().stream().filter(el -> Integer.parseInt(el.getAsJsonObject().get("AA_YMD").getAsString()) >= date).filter(el -> el.getAsJsonObject().get("EVENT_NM").getAsString().contains("방학")).limit(1).toList().getFirst().getAsJsonObject();
                        //noinspection DuplicatedCode
                        String dateString = o.get("AA_YMD").getAsString();
                        //noinspection MagicConstant, deprecation
                        long subDate = new Date(Integer.parseInt(dateString.substring(0, 4)) - 1900, Integer.parseInt(dateString.substring(4, 6)) - 1, Integer.parseInt(dateString.substring(6))).getTime() / 1000;
						Date leftTime = new Date(subDate * 1000 - new Date().getTime());
                        event.reply("**\"" + o.get("EVENT_NM").getAsString() + "\"**은, <t:" + subDate + ":D>이야.\n" + (leftTime.getTime() / 86_400_000) + "일 " + (leftTime.getTime() % 86_400_000 / 3_600_000) + "시간 " + (leftTime.getTime() % 3_600_000 / 60000) + "분 " + (leftTime.getTime() % 60000 / 1000) + "초 남았고. (<t:" + subDate + ":R>)").queue();
					} catch (Throwable t) {
						t.printStackTrace(System.out);
						event.reply("아직 올라온 학사 일정이 없는 것 같네, 다음에 와.").setEphemeral(true).queue();
					}
				}
				case "다음쉬는날" -> {
					try {
						OptionMapping sat = event.getOption("토요일제외");
						JsonObject data = URLUtils.getSchoolSchedule(format(new Date(), ""), 1000);
						int date = Integer.parseInt(format(new Date(), ""));
						JsonObject o = data.getAsJsonArray("SchoolSchedule").get(1).getAsJsonObject().getAsJsonArray("row").asList().stream().filter(el -> Integer.parseInt(el.getAsJsonObject().get("AA_YMD").getAsString()) >= date).filter(el -> ((sat != null && !sat.getAsBoolean()) || !el.getAsJsonObject().get("EVENT_NM").getAsString().equalsIgnoreCase("토요휴업일")) && el.getAsJsonObject().get("SBTR_DD_SC_NM").getAsString().matches("공휴일|휴업일")).limit(1).toList().getFirst().getAsJsonObject();
                        //noinspection DuplicatedCode
                        String dateString = o.get("AA_YMD").getAsString();
                        //noinspection MagicConstant, deprecation
                        long subDate = new Date(Integer.parseInt(dateString.substring(0, 4)) - 1900, Integer.parseInt(dateString.substring(4, 6)) - 1, Integer.parseInt(dateString.substring(6))).getTime() / 1000;
						Date leftTime = new Date(subDate * 1000 - new Date().getTime());
                        event.reply("**\"" + o.get("EVENT_NM").getAsString() + "\"**은, <t:" + subDate + ":D>이야.\n" + (leftTime.getTime() / 86_400_000) + "일 " + (leftTime.getTime() % 86_400_000 / 3_600_000) + "시간 " + (leftTime.getTime() % 3_600_000 / 60000) + "분 " + (leftTime.getTime() % 60000 / 1000) + "초 남았고. (<t:" + subDate + ":R>)").queue();
					} catch (Throwable t) {
						t.printStackTrace(System.out);
						event.reply("아직 올라온 학사 일정이 없는 것 같네, 다음에 와.").setEphemeral(true).queue();
					}
				}
				case "학습" -> {
					try {
						OptionMapping in = event.getOption("입력메시지");
						OptionMapping out = event.getOption("출력메시지");
						if (in == null || out == null) throw new RuntimeException();
						if (in.getAsString().contains("루아야")) {
							event.reply("너도 이름 두번씩 불러줄까? " + event.getUser().getAsMention() + event.getUser().getAsMention()).setAllowedMentions(new ArrayList<>()).queue();
							return;
						}
						String s;
						if ((s = StringUtils.censor(in.getAsString())) != null) {
							event.reply("내가 그런 말 할 이미지가 아니라는건 알고 있지?\n-# 검열 내용: " + s).queue();
							return;
						}
						if ((s = StringUtils.censor(out.getAsString())) != null) {
							event.reply("내가 그런 말 할 이미지가 아니라는건 알고 있지?\n-# 검열 내용: " + s).queue();
							return;
						}
						if ((s = StringUtils.censor(in.getAsString() + out.getAsString())) != null) {
							event.reply("내가 그런 말 할 이미지가 아니라는건 알고 있지?\n-# 검열 내용: " + s).queue();
							return;
						}
						if (SQLUtils.addMessage(in.getAsString(), out.getAsString(), event.getUser().getId())) {
							event.reply("음, 이건 이미 알고있는 말이야.").queue();
							return;
						}
						event.reply("앞으로\n```\n" + in.getAsString() + "\n```\n(이)라고 물어보면\n```\n" + out.getAsString() + "\n```\n(이)라고 대답할게.").queue();
					} catch (Throwable t) {
						t.printStackTrace(System.out);
						event.reply("노트가 가득 찼어.").queue();
					}
				}
				case "학습확인" -> {
					try {
						User u = event.getUser();
						OptionMapping user = event.getOption("유저");
						if (user != null) u = user.getAsUser();
						SQLUtils.getMessageByUser(u.getId());
						EmbedBuilder builder = new EmbedBuilder().setTitle(u.getEffectiveName() + "님이 학습시킨 메시지");
                        for (MessageResponseData data : SQLUtils.getMessageByUser(u.getId()))
                            builder.appendDescription(data.inputMessage + " -> " + data.outputMessage + "\n");
						event.replyEmbeds(builder.build()).queue();
					} catch (Throwable t) {
						t.printStackTrace(System.out);
						event.reply("글쎄, 너무 오래 전이라 기억은 안 나는것 같은데.").queue();
					}
				}
				case "망각" -> {
					try {
						OptionMapping in = event.getOption("입력메시지");
						OptionMapping out = event.getOption("출력메시지");
						if (in == null || (out == null ? !SQLUtils.removeMessage(in.getAsString(), event.getUser().getId()) : !SQLUtils.removeMessage(in.getAsString(), out.getAsString(), event.getUser().getId()))) throw new RuntimeException();
						event.reply("```\n" + in.getAsString() + "\n```\n" + (out == null ? "" : "->\n```\n" + out.getAsString() + "\n```\n") + "이 정보는 나중에 지워버리도록 할게.").queue();
					} catch (Throwable t) {
						t.printStackTrace(System.out);
						event.reply("그런 걸 알려준 적이 있었나?").queue();
					}
				}
				default -> event.reply("무슨 명령어를 쓰려는 거야, 아직 만들지도 않았으니 조금만 기다려.").setEphemeral(true).queue();
			}
		}
		if (e instanceof MessageReceivedEvent event) {
			if (event.getAuthor().getId().equals("1250810431375806565")) {
				String sql = "SELECT * FROM `dsm_information`.`valbungg_message_log` WHERE input_message=?";
                //noinspection DuplicatedCode
                try (PreparedStatement statement = SQLUtils.getConnection().prepareStatement(sql)) {
					List<MessageResponseData> list = new ArrayList<>();
					statement.setString(1, event.getMessage().getContentRaw());
					ResultSet result = statement.executeQuery();
					while (result.next())
						list.add(new MessageResponseData(result.getString("input_message"), result.getString("output_message"), "0"));
					if (!list.isEmpty()) {
						MessageResponseData data = list.get(DSMInformation.random.nextInt(0, list.size()));
						event.getMessage().reply(data.outputMessage).setAllowedMentions(new ArrayList<>()).mentionRepliedUser(false).queue();
					}
				} catch (SQLException ex) {
					ex.printStackTrace(System.out);
				}
				return;
			}
			if (event.getAuthor().isBot() || event.getAuthor().isSystem()) {
				String sql = "SELECT * FROM `dsm_information`.`bot_message_log` WHERE input_message=?";
                //noinspection DuplicatedCode
                try (PreparedStatement statement = SQLUtils.getConnection().prepareStatement(sql)) {
					List<MessageResponseData> list = new ArrayList<>();
					statement.setString(1, event.getMessage().getContentRaw());
					ResultSet result = statement.executeQuery();
					while (result.next())
						list.add(new MessageResponseData(result.getString("input_message"), result.getString("output_message"), "0"));
					if (!list.isEmpty()) {
						MessageResponseData data = list.get(DSMInformation.random.nextInt(0, list.size()));
						event.getMessage().reply(data.outputMessage).setAllowedMentions(new ArrayList<>()).mentionRepliedUser(false).queue();
					}
				} catch (SQLException ex) {
					ex.printStackTrace(System.out);
				}
				return;
			}
			label: if (event.getMessage().getContentRaw().contains("루아야") || event.getMessage().getMentions().isMentioned(event.getJDA().getSelfUser())) {
				if (event.getMessage().getContentRaw().equalsIgnoreCase("루아야") || event.getMessage().getContentRaw().equalsIgnoreCase("<@1345676526154158131>")) {
					event.getMessage().reply("응, 불렀어?").mentionRepliedUser(false).queue();
					break label;
				}
				var list = SQLUtils.getMessage(event.getMessage().getContentRaw().replaceAll(" ?루아야 ?| ?<@1345676526154158131> ?", "").trim());
				if (!list.isEmpty()) {
					MessageResponseData data = list.get(DSMInformation.random.nextInt(0, list.size()));
					event.getMessage().reply(data.outputMessage + (!data.userId.equals("419137051670347777") && !data.userId.equals("1285566586257674240") ? "\n-# " + User.fromId(data.userId).getAsMention() + "(이)가 가르쳐 줬어." : "")).setAllowedMentions(new ArrayList<>()).mentionRepliedUser(false).queue();
					break label;
				}
				event.getMessage().reply("음, 그런 말은 배운 적 없어.").mentionRepliedUser(false).queue();
			}
			var list = SQLUtils.getNormalMessage(event.getMessage().getContentRaw());
			if (!list.isEmpty()) {
				MessageResponseData data = list.get(DSMInformation.random.nextInt(0, list.size()));
				event.getMessage().reply(data.outputMessage).setAllowedMentions(new ArrayList<>()).mentionRepliedUser(false).queue();
			}
		}
	}
}
