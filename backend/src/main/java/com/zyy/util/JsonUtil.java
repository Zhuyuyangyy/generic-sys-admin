package com.zyy.util;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ==========================================================
 * 忙拢拢氓聠篓忙卤虏 氓娄陇氓聜聸卯聼聡忙楼聽氓聸卢氓麓職氓篓聭忙卢聬莽聟聙茅聳禄忙聞庐忙芦垄莽禄卤忙聣庐莽麓聮茅聬聰氓聣聛卯聛卤 JSON 莽聙鹿忙聬聻氓聞卤茅聧聶猫路篓莽聦戮忙碌录忙卢聭莽陇聞JsonUtil茅聳驴? * ==========================================================
 *
 * 忙拢拢氓聠篓莽庐職 氓篓聭忙聮鹿忙隆篓莽禄庐氓聽聼莽篓聤茅聧芦茫聝娄盲禄聵茅聬聼忙聸職忙碌聡莽禄禄忙聽篓莽篓聣茅隆聫氓聴職卯聺娄茅聳驴? *
 *   Java 茅聳禄茫聢垄氓聳聬茅聢搂卯聝驴莽路職茅聧拢?JSON 茅聨录氓颅聵忙聦赂茅聤聡氓漏聝氓戮聞氓搂聵卯聜陇氓聲聤茅聳驴? *   - Fastjson2茅聳驴忙露聵莽聣聯氓娄炉氓卢庐忙聼聦氓庐聙氓卢卢莽陇聣茅聳鹿卯聝卢氓聴聴氓聨麓氓娄陇氓聜聸茂鹿芦莽禄卤忙驴聥忙聼聢茅聤聤茫聝楼莽聙禄茅聳赂茂赂戮氓聞陇氓篓聟忙卢聯氓录碌?bug茅聳驴? *   - Jackson茅聳驴忙露聵忙鹿炉pring 茅聳赂氓驴聥氓聤聦茅隆聰氓露聟卯聸录茅聧芦忙聸聽氓赂聸茅聬聽盲陆碌氓聙搂莽禄卤忙驴聥忙聜陆茅聬聰莽鲁聲氓聛聯忙碌拢猫搂聞盲禄聵茅聳赂氓驴拢莽聟聣莽禄卤忙驴聢莽虏聶茅聧聬猫聦卢忙職掳茅聳驴? *   - Gson茅聳驴忙露聵忙鹿聶oogle茅聳驴忙露聵莽聼聛忙碌聹茫聜聣忙聼聦猫鹿聡茫聝搂莽陇聣氓篓麓茂陆聟忙拢聴氓篓麓忙楼聟氓麓聵茅聧聸卯聢聸忙職聫莽聙碌莽聣聢卯職聟茅聬篓卯聢聻忙聲聧? *   - Hutool JSONUtil茅聳驴忙露聵莽聣聠茅隆聲卯聠陆莽篓聣忙驴聽氓聭颅莽聣職茅聬聫氓驴聨忙碌聡茅隆楼氓漏聡忙聲聧莽聙聸氓隆聮I 茅聳潞氓聽職氓聛聯茅聳赂忙卢聫卯聝聟茅聤聢盲禄聥忙聲聧? *
 *   茅聳潞氓聽芦氓聣聼茅聤聠氓露聣忙聝聞茅隆聲芒聜卢茅聢搂卯聞聙卯聵炉茅聬聨莽聝陆忙聲聧氓篓聠莽娄路stjson2茅聳驴忙露聵莽聣聫茅聫聛茫聜聠莽娄聧?Map/List 茅聳禄茫聞楼氓芦卢茅聨禄芒聲聟忙聼聟茅聬聰忙聛聧忙聨聴氓录赂茅聨掳氓聣聛莽陇聠+ Jackson茅聳驴忙露聵莽聣聫茅聫聛茫聜聠莽娄聧忙聬麓芒聵聠忙搂禄茅聳潞氓陇聤氓聙赂茅隆聲卯聠戮忙聦聮茅聰聸氓聥卢莽聲卤茅聨录忙聺聭莽鹿聬茅聧篓卯聛聢氓麓聽茅聫聧莽聰碌莽陇聠
 *   Hutool JSONUtil 氓篓麓茂陆聠莽虏聢莽聮聬莽聠录忙聡聴茅聭赂氓聻芦氓炉聹茅聬聫莽聜虏氓聛聤莽禄卤忙驴聢莽麓聮茅聬聰氓聣聛卯聛卤茅聬聨莽聰碌忙聡聯茅隆娄氓漏職氓录鲁茅聨戮卯聠陆猫聥聼茅聳潞氓聽職氓聛聯莽录聛莽聤芦氓聛聯忙驴聻猫鹿颅猫聝露氓篓聢?API茅聳碌? *
 * ==========================================================
 *
 * 忙拢拢氓聠篓莽庐職 茅聬聽盲陆鹿氓麓聵茅隆聯忙聬聛氓麓垄茅聬聰氓鲁掳莽聛聼茅聳驴忙露聵莽聣聯茅聬聺卯聡聙氓麓聵茅聧聸茅垄聛莽虏碌茅聳驴忙露聵莽聼聜氓篓录忙聣庐忙聥聽茅聫聡莽聜虏猫庐虏茅聳赂忙掳卤忙芦垄莽禄卤忙掳露忙聲聧? *
 *   茅聳碌氓聴聵氓聤聲氓庐聲莽聠赂莽篓聣茅聢搂卯聞聞忙聡聲氓庐聙氓聠陋莽聟聭茅聳碌氓聴聵氓聻露茅聬娄芒聞聝莽篓聣茅隆聫氓聭庐莽聟聶忙驴聻氓陇聥忙麓聳猫陇掳脩聡氓麓聭氓搂聵卯聜陇卯聛卤氓篓麓莽聤鲁忙鹿鹿莽禄篓茫聞漏忙聲聧莽聛聻猫聜漏莽聭聺茅聴聛忙聫聮莽搂麓茅隆娄? *   茅聳碌氓聴聵氓聤聶氓娄虏猫炉虏卯聡楼茅聢漏氓聠拢氓聛聯猫陇聫莽禄卤卯聛聠莽虏聶莽聙拢茫聞垄氓聛聯茅聨麓忙驴聠忙拢聵茅聳潞盲陆潞氓垄聴茅聬聴氓潞垄卯職楼莽聛聻猫聴聣卯聳聟莽录聜氓聠戮氓聶炉莽禄卤忙驴聥氓麓拢茅聧聦忙露聶忙庐露氓篓聭忙聮鲁莽搂麓茅隆聲卯聡聙忙聝聞莽聙聸忙篓潞氓陇聧茅聳鹿猫聢碌莽篓聭莽禄卤忙聮露忙聲庐茅聴聢芒聲聝莽陇聣氓篓聭忙聮鲁莽搂麓莽禄露忙掳露氓麓楼忙聬麓茫聜聢忙鲁虏
 *   茅聳碌氓聴聵氓聤聴莽禄庐茫聞漏氓录赂氓漏聤氓聭炉氓聼聽氓篓聭忙聮炉氓聛聯茅聳碌氓聴聵氓聻录莽禄禄忙聢娄氓麓露茅聬聬莽聰碌忙鲁垄茅聳潞氓卢芦莽虏聫茅隆漏茂赂陆莽篓聤茅聧芦莽聠赂脨娄茅聬聼忙聸職猫聝露氓篓聢忙聢娄氓麓聬莽潞颅茅拢聨莽陇聣茅聬聼忙聸職忙陇聮莽禄聽莽聜潞氓录掳?Optional茅聳驴忙露聵氓楼聰莽禄聴氓陇聥忙陆禄茅聫聜忙聦聨莽陇聙 null
 *   茅聳碌氓聴聵氓聤聶茅聫聛氓漏聞忙聥聽茅隆聳忙露聶茂录聣茅聼芦氓聸篓茫聛聹茅聢搂卯聞聛氓聻露忙戮搂氓露聣氓录碌忙戮露氓漏聞莽拢陆茅聬垄卯聢職忙聠隆忙戮搂茂鹿聜氓麓隆茅聬聴氓聽聼茂录聣茅聼芦氓聸篓卯職聬莽禄卤忙驴聥氓录卢茅聳芦忙露職芒聰聮茅聳鹿莽聝聵氓赂聴茅聬聯茂驴聽忙聲聧莽聛聻猫聜漏莽篓禄氓篓聭忙聮鲁莽搂麓茅聨庐氓聽聲氓鹿聮? *
 * ==========================================================
 *
 * 忙拢拢氓聠篓莽庐職 茅聬聨氓鹿驴氓聻潞茅聧漏忙露聶忙聥聟莽聮潞茫聞娄忙職聫莽录聛芒聜卢忙聺聻忙聣庐盲录聬茅聳驴忙露聵莽聣聫氓篓虏氓聺聴氓鹿聮茅聤聣茫聝庐忙搂禄茅聳赂忙聨聭氓垄聨茅聬颅忙篓录忙聥鹿忙聺聢氓聣搂莽陇聠茅聳驴? *
 * ```java
 * // 1. 茅聬聨莽聰碌卯聰聤茅聮聳氓聥卢忙陆陋?JSON 茅聬聨忙露聶卯職聢茅隆聭盲陆鹿莽篓聣? * User user = new User(1L, "莽聙碌卯聠戮氓搂鲁莽禄聴?, "123@qq.com");
 * String json = JsonUtil.toJson(user);
 * // {"id":1,"name":"莽聙碌卯聠戮氓搂鲁莽禄聴?,"email":"123@qq.com"}
 *
 * // 2. JSON 茅聬聨忙露聶卯職聢茅隆聭盲陆鹿莽篓聣茅聭录猫聢碌莽楼庐茅聬聨莽聰碌卯聰聤茅聮聳? * User u = JsonUtil.fromJson(json, User.class);
 *
 * // 3. JSON 茅聫聣?List
 * String jsonArr = "[{\"name\":\"氓篓聭忙聴聭忙炉聙\"},{\"name\":\"氓篓聭忙聴聭忙炉聝\"}]";
 * List<Book> books = JsonUtil.fromJsonArray(jsonArr, Book.class);
 *
 * // 4. 氓篓麓莽聤虏茅陋赂茅隆娄忙聼楼氓录露?JSON 茅聴聛忙聫聮猫聥炉猫陇掳氓聸搂脟聨茅聧聴莽聜碌忙鹿麓茅聬聨忙露聶卯職聠茅隆聰氓虏聞忙聲聧茅聧芦忙禄聟莽聭聺茅聳禄卯聺卢氓聤聭忙碌聹氓聽聲氓麓垄茅聧隆卯聟聽莽陇聠
 * String resp = "{\"code\":200,\"data\":{\"user\":{\"name\":\"茅聳潞氓陇聤茅陋赂氓篓虏忙聫卢"}}}";
 * String name = JsonUtil.getString(resp, "data.user.name");
 * // 莽录聜盲陆鹿忙聦禄茅聬聫氓陇聬忙聲聧氓搂聵卯聟聼莽露聧茅聳赂茫聝娄氓聠禄莽禄卤忙卢聭莽聦戮莽聮聛猫聶芦氓娄搂 JS 茅聳禄?object.data.user.name茅聳驴? *
 * // 5. 茅聳赂忙聨聥氓聙聳茅聫聦?JSON 茅聳潞氓聥碌氓聻掳茅聨炉盲戮聙氓麓楼茅聧芦莽聠潞茫聙聤
 * boolean valid = JsonUtil.isValidJSON(jsonString);
 *
 * // 6. 茅聳潞氓露聡氓聨搂莽禄卤茂驴聽氓麓聽茅聫聧莽聰碌莽陇聞莽录聜氓聸搂茅陋赂莽聙碌忙聼楼氓鹿聧茅聨戮氓聠虏莽楼陋茅聳驴忙露聵莽聼聛茅聬篓莽聠潞忙聥聽茅聫聡莽聠赂卯聵搂茅聳禄茫聢漏莽聟聣莽禄卤? * String pretty = JsonUtil.format(user);
 * System.out.println(pretty);
 * // {
 * //   "id" : 1,
 * //   "name" : "莽聙碌卯聠戮氓搂鲁莽禄聴?
 * // }
 * ```
 *
 * ==========================================================
 *
 * @author Alice 忙拢拢氓聠篓莽垄聧
 */
public final class JsonUtil {

    private static final Logger log = LoggerFactory.getLogger(JsonUtil.class);

    /**
     * 忙拢拢氓聠篓忙卤虏 茅聴聜氓聽聼莽聭娄茅聢搂卯聝驴莽陇聛氓庐聲莽聠赂莽卢聼莽聙拢氓聣聛莽陋聴JackJson ObjectMapper茅聳驴忙露聵莽聣聫茅聧陇氓潞拢莽虏聶莽聙拢卯聛聠忙職聰茅聳赂氓驴拢莽聟聣莽禄卤忙驴聥氓麓拢茅隆聳忙掳录忙搂禄茅聳禄茫聢漏莽聟聣莽禄卤?     *
     * 茅聳碌氓聴聵氓聤聮莽聮聬莽聠赂莽娄聮茅聢搂卯聞聛莽篓聤茅聧芦茂陆聨忙麓拢茅聳赂忙楼聛忙麓聹莽聙職卯聟垄氓麓職氓篓聭忙篓录莽麓娄氓篓聭忙聮炉氓聛聯氓篓聭?ObjectMapper茅聳驴忙露職氓聳聠茅聢搂?     * - Jackson 茅聳禄?API 茅聳赂忙聨聭忙拢聶茅聫聠氓潞篓忙聲聧氓庐聙芒聜卢氓篓虏氓聺聴氓鹿聮茅聤聣茫聞搂忙庐露莽聙碌忙聺驴莽聣聫莽禄聽忙聨聴忙聜露?     * - 茅聳赂忙聨聯莽篓聭莽录聜?ObjectMapper 茅聳潞氓聽聺卯聵庐莽禄聰氓炉赂芒聜卢猫搂聞猫聣戮莽禄卤忙聢娄忙聼篓茅聢搂卯聞聞忙聲聧茅聧芦莽聠路卯聴聡茅聳鹿猫炉虏莽鹿聮莽聬職卯聜娄氓娄麓忙碌拢猫聶鹿氓陇聞茅聬聨忙露聶脙陋莽禄篓卯聟垄氓麓職氓娄陇芒聜卢莽聙碌忙聼楼氓麓鲁茅隆颅忙聨聰莽陇聠
     * - 氓漏垄氓聹颅氓垄聴氓娄虏忙聢娄氓鹿聥?static 茅聳赂忙卢聬茂鹿垄茅聧拢忙麓陋忙聲聧莽聛聻猫聴聣氓录驴茅聬聫莽聜漏氓聛聯氓漏垄猫路潞莽搂露茅聫聛茫聜聠忙陆禄氓篓聠氓潞拢卯聛卤氓篓聭忙聯聝盲潞聹茅聬陇氓聥卢莽卢聼莽聙拢氓聣聛莽陇聣茅聳录氓聯聞氓聙禄氓篓聯茅拢聨忙聦搂茅聧聲卯聞聜莽聢卤
     *
     * 茅聳碌氓聴聵氓聤聶茅聧聵茫聜聡莽麓聰茅隆聰忙卢戮氓職聸茅聳潞氓聥芦莽掳潞茅聢搂卯聞聛氓聼聤莽禄卤?     * - JavaTimeModule茅聳驴忙露聶莽垄聧茅聫聛卯聡聙氓鹿聬?Java8 茅聳潞氓聠篓莽聢录氓娄芦猫路篓莽聦戮莽聮聡猫聦卢芒聜卢莽聝陆忙聲聧茅聧娄莽聢聥calDateTime茅聳驴忙露聵卯聵炉莽禄篓卯聟垄氓麓職氓娄陇芒聜卢莽聙碌?     * - DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES = false茅聳驴?     *   JSON 茅聴聛忙聫聮忙聹卢氓漏聙盲陆潞芒聜卢忙露聶卯職聠茅隆聰氓卤戮忙聥聟?Java 忙驴聻氓卤赂氓聣聺氓漏聙盲戮聙忙聲聧莽聛聻猫聤楼氓芦鹿茅聳禄茂陆聝氓聞卤茅聬拢莽聤禄忙聲聧莽聛聻猫聜漏莽聭聺茅聳鹿猫潞虏氓聞碌茅聫聛氓漏聡忙聲聧茅聧芦莽聠路氓陇聧茅聳赂忙卢聶莽碌陆氓庐聲氓虏聙莽聴陋猫陇聨氓娄聻氓聜聻氓录碌忙戮露氓卤戮忙職聫茅聳驴?     * - SerializationFeature.WRITE_DATES_AS_TIMESTAMPS = false茅聳驴?     *   LocalDateTime 茅聨录忙聺聭莽鹿聬茅聧篓卯聛聢氓麓聽茅聫聧茫聞楼莽聛聡 "2026-04-19T11:00:00" 茅聳录忙聺驴氓楼聰莽禄聴氓陇聬氓录掳?[2026,4,19,...]
     */
    private static final ObjectMapper MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    // ==================== 莽聙鹿忙聬聻氓聞卤茅聧聶猫路篓莽聦戮茅聭陆茫聜聨忙陆聦茅聳潞氓聽聺卯聵掳茅聬聨卯聢聻忙聼聟茅聬聵忙聫聮忙炉聬茅聳潞?====================

    private JsonUtil() {
        // 莽聙鹿忙聬聻氓聞卤茅聧聶猫路篓莽聦戮莽聮聛氓麓聡莽聭聺茅聳赂氓驴聨忙碌聡茅隆聰氓驴聲芒聜卢氓聹颅氓聛聵莽禄露茫聝漏氓麓聽茅聫聧莽聰碌莽陇聣莽聙碌卯聠录忙聬芦茅聧漏忙楼聛忙聥聟莽聮潞茫聞娄忙職聫茅聴聜氓聽聼莽聭娄茅聢搂卯聝驴莽聞娄茅聫聦莽聠路芒聳聰?        throw new UnsupportedOperationException("JsonUtil 茅聳潞氓聥碌氓聻掳忙碌录忙聞掳氓麓聴忙陇聥氓潞陇卯聺娄茅聳驴忙露聵氓楼聰莽禄聴氓陇聬氓麓聴忙碌拢莽颅聥氓聲聟茅聬聨氓聹颅氓聛聵莽禄露茫聝漏氓麓聽?);
    }

    // ==================== 茅聳潞氓露聡卯聼聢莽禄潞茅鹿聝氓录卢莽聭聶氓聥颅茫聙聤茅聳驴忙露聶猫聣戮莽禄篓卯聟垄氓麓職氓娄陇芒聜卢莽聙碌?====================

    /**
     * 忙拢拢氓聠篓忙卤虏 茅聬聨莽聰碌卯聰聤茅聮聳?茅聳鲁?JSON 茅聬聨忙露聶卯職聢茅隆聭盲陆鹿莽篓聣莽聮聡脩聟莽陇聞Jackson茅聳驴忙露聵忙聹卢茅聢搂卯聝卢氓聴聴氓聨麓茅聳潞氓聽職氓聛聯氓篓麓氓聟录茂鹿芦莽禄卤?     *
     * 茅聳碌氓聴聵氓聤聲茅隆聲卯聠录脨聮?Fastjson2.toJSONString茅聳碌?     * - Fastjson2 茅聳赂茂赂潞氓聤聦茅隆娄忙聼楼氓录露茅聧聦忙掳录氓職聽茅聬聽莽聜漏忙搂聞莽禄卤忙卢聭盲潞赂茅聤聢氓聭颅氓戮聯茅聳潞氓聥碌氓聻掳茅聨炉氓聽聺芒聳聰氓篓聭忙篓录芒聜卢莽聝陆忙聲聧忙戮露氓卢芦卯聵搂茅聳赂氓卢颅忙鲁聸猫陇掳氓聜聸忙聝聰猫鹿聡忙聮鲁莽聛聶茅聳赂忙聽篓莽聣聲莽禄卤忙聮露忙聲庐?     * - Jackson 茅聳潞氓聸搂卯聡颅猫聫聶茅聬聨猫搂聞莽聠卢莽禄卤忙驴聥忙聜陆茅聬聰莽鲁聲氓聛聯忙碌拢猫搂聞莽潞炉忙楼聽莽聜虏氓掳聟莽禄卤忙录聡pring 氓搂聮忙露聶卯聼聢茅隆聯?     * - 茅聫聣芒聲聜莽聭漏茅聧拢茅拢聨莽麓聮茅聬聰氓聣聛卯聛卤茅聳禄?Jackson茅聳驴忙露聵猫聥炉茅隆聲卯聠陆氓戮聞茅聫聧卯聟聼卯聟潞茅聳陆忙篓潞莽鹿聬茅聬陇氓聥炉忙聜鲁茅聨碌卯聠戮莽聼聨茅聳录?     *
     * @param obj 氓篓麓莽聤碌莽虏炉茅聧聯忙聣庐芒聜卢莽聰碌卯聰聤茅聮聳氓聥炉忙聲聧茅聧娄卯聶聲JO / Map / Collection / 茅聳赂芒聲聝氓聞陇氓漏聙忙聣庐莽聦戮莽聮聡猫聦卢芒聜卢莽聝陆忙聲聧?     * @return JSON 茅聬聨忙露聶卯職聢茅隆聭盲陆鹿莽篓聣?     * @throws RuntimeException 茅聨录忙聺聭莽鹿聬茅聧篓卯聛聢氓麓聽茅聫聧脩聛盲潞录茅聬聽忙聞漏氓聞虏氓娄聻氓聜聻氓鹿聨氓篓聭忙篓潞忙炉聣
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("[JsonUtil] 茅聬聨莽聰碌卯聰聤茅聮聳氓聥卢忙陆陋?JSON 氓漏垄忙聝掳莽虏戮莽聭聶?| Object: {} | Error: {}", obj, e.getMessage());
            throw new RuntimeException("JSON 茅聨录忙聺聭莽鹿聬茅聧篓卯聛聢氓麓聽茅聫聧脩聛盲潞录茅聬聽? " + e.getMessage(), e);
        }
    }

    /**
     * 忙拢拢氓聠篓忙卤虏 茅聬聨莽聰碌卯聰聤茅聮聳?茅聳鲁?莽录聜氓聸搂茅陋赂莽聙碌?JSON 茅聬聨忙露聶卯職聢茅隆聭盲陆鹿莽篓聣莽聮聡脩聟莽陇聞茅聳潞氓露聡氓聨搂莽禄卤茂驴聽氓麓聽茅聫聧莽聰碌莽陇聣氓篓聯忙掳卢茫聛聵莽禄篓卯聞聜忙聥芦茅聧聬卯聟聽忙搂赂茅聳驴?     *
     * 茅聳碌氓聴聵氓聤聮忙驴聻氓聸卢忙聜陆茅聤聤茫聝娄莽掳職茅聳潞氓聮聛氓聻卢茅聢搂?     * - 茅聳潞氓聠娄氓聞卤莽禄禄忙聴聙忙陆聫茅聨戮氓聠虏忙炉聣茅聳驴忙露聵忙聹卢忙戮搂茂鹿聜氓麓隆茅聫聣氓聻庐忙炉聣茅聳潞氓陇聢氓聞卤茅聬陋忙聺驴芒聳聨茅聢楼氓聠虏莽聙禄茅聳潞?     * - 茅聳鹿忙聛聮氓聞卤猫陇掳忙露聶忙陆禄茅聫聜忙聦聨莽陇聙茅聳赂忙聞卢忙聡聨茅隆聰?debug茅聳驴忙露聵猫聥炉猫陇掳猫聦卢忙聥聽莽录聛忙篓路氓聛聯猫陇聰茅聬聺?     * - 氓篓拢氓聸篓莽鹿聜茅聬隆茫聞漏忙聼聤氓庐楼氓聸漏忙聻聜茅聳赂忙聨聰氓垄聴茅聫聥氓聠漏莽娄聮莽聮聛氓聹颅莽陇聞茅聳潞氓露聡氓聨搂莽禄卤茂驴聽氓麓聽茅聫聧脩聞氓聙碌氓篓麓忙禄聢莽录職茅聧聫忙漏聙忙聝聡莽聙拢卯聜垄忙芦聬茅聳驴?     *
     * @param obj 氓篓麓莽聤碌莽虏炉茅聧聯忙聣庐芒聜卢莽聰碌卯聰聤茅聮聳?     * @return 茅聳潞氓露聡氓聨搂莽禄卤茂驴聽氓麓聽茅聫聧脩聞氓聙碌茅聳禄?JSON茅聳驴忙露聵莽聣聫莽录聜氓陇聥忙陆禄?2 莽录聛氓虏聙氓聞陇茅聬聴忙聬聛忙聲聧?     */
    public static String format(Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("[JsonUtil] 茅聳潞氓露聡氓聨搂莽禄卤茂驴聽氓麓聽?JSON 氓漏垄忙聝掳莽虏戮莽聭聶?| Error: {}", e.getMessage());
            throw new RuntimeException("JSON 茅聳潞氓露聡氓聨搂莽禄卤茂驴聽氓麓聽茅聫聧脩聛盲潞录茅聬聽? " + e.getMessage(), e);
        }
    }

    // ==================== 茅聳潞氓露聡卯聼聢莽禄潞茅鹿聝氓录卢莽聭聶氓聥颅茫聙聤茅聳驴忙露聶猫聣戮氓炉庐猫聤楼忙聝聰猫鹿聡忙聮鲁莽聛聶茅聳赂?====================

    /**
     * 忙拢拢氓聠篓忙卤虏 JSON 茅聬聨忙露聶卯職聢茅隆聭盲陆鹿莽篓聣?茅聳鲁?POJO 茅聬聨莽聰碌卯聰聤茅聮聳?     *
     * 茅聳碌氓聴聵氓聤聳莽潞颅茅鹿聝氓麓鹿莽聙拢卯聞聙卯聲录茅聳潞?vs 茅聴聜氓聽垄氓聛聻莽潞颅茅鹿聝氓麓鹿莽聙拢卯聞聙卯聲录茅聳潞氓聽芦氓聟聶茅聢搂?     * - 茅聴聜氓聽垄氓聛聻莽潞颅茅鹿聝氓麓鹿莽聙拢氓聣聛莽陋聴fromJson(json, User.class) 茅聳鲁?茅聫聣芒聲聜忙聼篓氓篓虏?User茅聳驴忙露聵莽聣聫茅聬拢忙驴聠脙潞忙赂職氓漏聞莽陇聣氓篓麓茂陆聟忙拢聰氓篓聭卯聛聟氓戮聞忙聺聢茫聞搂茫聙聬茅聳赂茫聞楼卯聝聭莽禄卤?     * - 忙驴聻氓陇聥莽篓聭茅聬聨莽聝陆忙聲聧氓搂聺莽聢聨omJson(json, new TypeReference<List<User>>(){}) 茅聳鲁?茅聫聣芒聲聜忙聼篓氓篓虏?List<User>
     * - 茅聫聣芒聲聜莽聭漏茅聧拢莽聝陆氓麓拢茅隆聫氓聭颅莽碌鹿氓篓聯忙掳颅莽聸炉氓篓录卯聛聞芒聳聰氓篓聭忙篓录芒聜卢莽聝陆忙聜搂茅聧芦莽聠赂忙聥卤茅聳驴忙露聵忙聹卢莽潞颅茅鹿聝氓麓鹿莽聙拢卯聞聙卯聲录茅聳潞氓聽芦氓聣聸茅聫聛茅聛聧忙聼聦氓庐楼氓聽聼莽楼掳茅聳潞氓聜聺卯聺聣莽潞颅猫聢碌氓戮聞茅聧聲卯聟聼氓聙聻
     *
     * @param json JSON 茅聬聨忙露聶卯職聢茅隆聭盲陆鹿莽篓聣?     * @param clazz 茅聳禄芒聲聤氓聳聴茅聬聳茂陆聡莽聦戮莽聮聡猫聦卢芒聜卢莽聝陆忙聲聧茅聧娄忙聵鹿ass茅聳驴?     * @param <T> 忙驴聻氓陇聥莽篓聭茅聬聨?     * @return 茅聳禄芒聲聤氓聳聴茅聬聳茂陆聡芒聜卢莽聰碌卯聰聤茅聮聳?     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("[JsonUtil] JSON 茅聫聣莽聝聠氓聣聶茅隆聲卯聠戮忙聦聮茅聢楼莽聜陆盲潞录茅聬聽?| JSON: {} | TargetClass: {} | Error: {}",
                json, clazz.getName(), e.getMessage());
            throw new RuntimeException("JSON 茅聳赂忙卢聫莽搂麓莽禄篓卯聟垄氓麓職氓娄陇芒聜卢莽聙碌氓聫聣氓戮聞忙聺聢卯聜楼脨聲: " + e.getMessage(), e);
        }
    }

    /**
     * 忙拢拢氓聠篓忙卤虏 JSON 茅聬聨忙露聶卯職聢茅隆聭盲陆鹿莽篓聣?茅聳鲁?氓漏垄猫路潞莽搂碌氓篓录氓聭炉莽聦戮莽聮聡猫聦卢芒聜卢莽聝陆忙聲聧茅聧芦莽聠赂忙職聹茅聳鹿茅聲聬莽聞娄莽潞颅茅鹿聝氓麓鹿莽聙拢卯聠陆猫聜聽茅聳赂忙掳卢莽聣聻莽禄卤?     *
     * 茅聳碌氓聴聵氓聤聮莽聮聬莽聠赂莽娄聮茅聢搂卯聞聛莽篓聤茅聧芦茂陆聨忙麓拢 new TypeReference茅聳驴忙露職氓聳聠茅聢搂?     * - Java 茅聳禄茫聞楼氓芦颅莽潞颅茅鹿聝氓麓鹿莽聙拢卯聜拢忙聠鹿茅聴聜氓聥漏氓聙聲茅隆聲茅聛聧忙聡聸忙聺聢氓聣搂莽陋聴List<User>.class 氓篓聭忙聮鲁莽搂麓茅聬隆茫聞漏氓麓路?     * - TypeReference 茅聳赂忙篓录氓聳聯茅聨庐氓漏聡忙聲聧氓搂聵猫戮篓忙職聫茅聬聨忙露聶氓聤聴莽聬職卯聜陇莽麓聮猫陇聨忙戮鹿忙卢聬莽漏卤氓漏碌氓聽聼忙庐聦氓篓麓忙禄聝忙拢聶莽潞颅茅鹿聝氓麓鹿莽聙拢卯聞聝卯聺娄茅聳赂茫聞楼卯聝聞忙路聡氓漏聡氓鹿聛?     * - 氓篓麓茂陆聢忙鲁聲茅聫聛茫聜聡莽虏聢忙聺聻忙聣庐盲录聬茅聳驴忙露聶莽聯虏romJson(json, new TypeReference<List<User>>(){}) 茅聳鲁?List<User>
     *
     * @param json JSON 茅聬聨忙露聶卯職聢茅隆聭盲陆鹿莽篓聣?     * @param typeRef 莽录聛卯聜楼卯聡搂茅聬聨氓鲁掳卯聡拢茅聫聡莽聤鲁忙職聫茅聳驴忙露聵忙陆卤ew TypeReference<T>(){}茅聳驴?     * @param <T> 茅聳禄芒聲聤氓聳聴茅聬聳茂陆聡莽聦戮莽聮聡猫聦卢芒聜卢?     * @return 茅聳禄芒聲聤氓聳聴茅聬聳茂陆聡芒聜卢莽聰碌卯聰聤茅聮聳?     */
    public static <T> T fromJson(String json, TypeReference<T> typeRef) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            log.error("[JsonUtil] JSON 茅聫聣莽聝聠氓聣聶茅隆娄忙聼楼氓录露茅聧聦忙禄聠卯聺娄茅聳赂茫聞楼卯聝聟茅聤聡忙聢聽忙聥鹿?| JSON: {} | TypeRef: {} | Error: {}",
                json, typeRef.getType(), e.getMessage());
            throw new RuntimeException("JSON 茅聳赂忙卢聫莽搂麓莽禄篓卯聟垄氓麓職氓娄陇芒聜卢莽聙碌氓聫聣氓戮聞忙聺聢卯聜楼脨聲: " + e.getMessage(), e);
        }
    }

    /**
     * 忙拢拢氓聠篓忙卤虏 JSON 茅聬聨忙露聶卯職聢茅隆聭盲陆鹿莽篓聣?茅聳鲁?List<T>
     *
     * 茅聳碌氓聴聵氓聤聮莽聮聬莽聠赂莽娄聮茅聢搂卯聞聛莽篓聤茅聧芦忙禄聟莽聭聺茅聳禄?fromJson 茅聴聛忙聫聮莽搂路氓篓麓氓聸卢忙聲聧茅聬聰莽虏鹿氓聛聯?     * - 茅聳禄芒聲聛莽聯篓莽聰炉氓颅聵氓炉录?Class 茅聳潞氓聠陋氓搂碌莽潞颅氓聣聛忙聜掳茅聤聤茫聞娄氓陆搂 List<User> 茅聫聣芒聲聜莽聭搂茅隆芦忙聨聰忙聲庐茅聰聰莽聲聦茫聙聬茅聳赂茫聞楼卯聝聡氓篓聢忙聢聽莽聦戮莽聮聡猫聦卢芒聜卢?     * - 茅聫聣芒聲聜莽聭漏茅聧拢茅聰聥莽篓聣茅聨戮莽禄聵茂录聦氓漏垄猫路潞氓芦庐茅聨庐氓漏聡忙聲聧氓庐聙氓聥陋莽录聣茅聳赂氓驴聯莽搂路茅聬篓莽聠录忙聜陆茅聤聤茂赂陆莽聟聶忙驴庐茂陆聟莽鹿聭茅隆聬氓楼赂忙聼聢茅聧聤莽聡聛忙麓拢茅聳赂?new TypeReference
     *
     * @param json JSON 茅聳潞盲陆鹿氓拢聠莽禄庐氓露聡芒聜卢忙露聶卯職聢茅隆聭盲陆鹿莽篓聣?     * @param clazz 茅聳赂忙聨聯卯職聣茅聤聠氓聠庐氓麓聴茅聧聬卯聛聠卯聵卤莽录聛卯聜楼卯聡搂茅聬聨?     * @param <T> 茅聳赂氓驴聯氓聲掳莽禄聙氓虏聙莽聦戮莽聮聡猫聦卢芒聜卢?     * @return List<T>
     */
    public static <T> List<T> fromJsonArray(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return MAPPER.readValue(json, MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (JsonProcessingException e) {
            log.error("[JsonUtil] JSON 茅聫聣?List 氓漏垄忙聝掳莽虏戮莽聭聶?| JSON: {} | ElementClass: {} | Error: {}",
                json, clazz.getName(), e.getMessage());
            throw new RuntimeException("JSON 茅聳赂忙卢聫莽搂麓莽禄篓卯聟垄氓麓職氓娄陇芒聜卢莽聙碌莽聺卤ist氓漏垄忙聝掳莽虏戮莽聭聶? " + e.getMessage(), e);
        }
    }

    // ==================== 茅聼芦氓聸露氓聛聟氓庐聯氓潞拢忙聥聥茅聧搂忙楼聛茂录聳茅聳潞氓聜聺卯聺聣莽潞颅氓聽聲忙聲聧?Hutool 茅聬聫氓驴聨忙碌聡茅隆楼氓漏聡忙聲聧?===================

    /**
     * 忙拢拢氓聠篓忙卤虏 氓篓麓?JSON 忙驴聻茂陆聟氓麓卢茅聬陋盲录麓氓麓拢茅聫聧脩聟忙聭聼忙驴聻氓聽垄卯聲庐莽禄卤忙卢聭莽聦戮莽聮聛猫聶芦氓娄搂 JS 茅聳禄?object.path.to.field茅聳驴?     *
     * 茅聳碌氓聴聵氓聤聮莽聮聬莽聠赂莽娄聮茅聢搂卯聞聛莽篓聤茅聧芦茂陆聨忙麓拢茅聫聣芒聲聜莽聭陇茅聳虏忙禄聢氓录卢莽聭聶氓聥颅茫聙聤茅聳驴忙露職氓聳聠茅聢搂?     * - 忙驴庐忙驴聯莽碌陆茅聬聳氓聽聲氓麓拢茅聫聧茫聞搂莽庐聮茅聬聫莽聜虏氓聙赂茅聬隆脩聝芒聳聯莽潞掳氓陇聦莽陋聴JSON.parseObject(json).getJSONObject("data").getJSONObject("user").getString("name")
     * - 茅聳禄卯聺卢氓聤聫莽禄禄忙聽篓莽篓聣茅隆聫氓聭庐莽聟聶忙驴聻氓陇聥忙麓漏莽禄卤莽聶聭sonUtil.getString(json, "data.user.name")
     * - 氓篓麓莽聥聟莽碌驴茅聬聳忙禄聞莽娄聮?4 茅聬聻忙露聵猫聥炉猫陇掳氓陇聬氓鹿聥?1 茅聬聻忙露聵莽聼聣莽禄卤忙驴聥氓麓拢茅隆聳忙驴聬氓職垄茅聳鹿卯聝卢氓聠娄盲潞拢忙楼聽莽聜虏忙聡聫猫陇掳盲戮聙氓麓隆?     *
     * 茅聳碌氓聴聵氓聤聲莽聰炉卯聜娄忙聜露茅聧隆忙聞篓氓聛聯?     * 氓篓麓茂陆聢忙鲁聲茅聫聛?Hutool JSONUtil 茅聳禄?getByPath 茅聳潞氓聜聺卯聺聣莽潞颅氓聽聲忙聲聧莽聛聻莽聜卢忙職聹茅聳鹿茅聲聬猫聝露茅聧聥茂陆聣氓麓拢茅聬陇卯聢聹莽聠聟莽聙碌忙聺驴氓芦炉茅隆聲茫聢聽芒聳聰?     *
     * 茅聳碌氓聴聵氓聤聮忙驴聻氓聸卢忙聜陆茅聤聤茫聝娄莽掳職茅聳潞氓聮聛氓聻卢茅聢搂?     * - 茅聬聽氓卢陋氓聲掳茅聫聛茫聜聡莽虏颅茅隆聭猫路篓莽聭聫茅聳潞?API 茅聳赂忙掳卢氓聡陆莽禄卤忙驴聥氓麓拢茅聫聧卯聞聜莽庐虏茅聳赂茫聝搂氓聛聸茅聢搂卯聞聙茫聜赂茅聧拢氓鲁掳莽聲碌莽聛聻莽颅聥忙庐掳莽聙碌忙聺驴莽聣聤莽禄禄盲戮聙忙聝聝茅聧聲卯聞聛忙庐露茅聳鹿?     * - 茅聳潞氓聠娄氓聞卤莽禄禄忙聴聜忙聥芦茅聧聬卯聟聽忙搂赂茅聳驴忙露聵忙聹卢茅聧聫氓聜聺莽庐聸茅隆聬茂赂鹿氓聛聯茅聬聰莽聝聵莽庐聶茅聳潞氓卤戮氓聤聮茅聳虏忙禄聟芒聜卢忙露聶卯職聠茅隆聰?     *
     * @param json JSON 茅聬聨忙露聶卯職聢茅隆聭盲陆鹿莽篓聣茅聧聶氓陇聤莽聛聴茅聬聨莽聰碌卯聰聤茅聮聳?     * @param path 茅聳禄忙聞庐忙聡聯猫陇掳氓聺聴氓麓職茅聧隆忙露聶卯聟戮茅聳禄茫聞楼氓芦炉茅聬颅忙聝搂卯聡楼茅聧聲氓聣聛莽陇聣氓漏碌?"data.user.name"
     * @return 茅聬聨忙露聶卯職聠茅隆聰氓虏聞氓麓聬莽潞颅茅拢聨莽陇聞茅聬聨忙露聶卯職聢茅隆聭盲陆鹿莽篓聣莽聮聡脩聟莽陇聠茅聳驴忙露聵氓楼聰莽禄聴氓陇聦芒聜卢忙露聶脙陋氓漏聙卯聛聟忙陆禄茅聫聜忙聦聨莽陇聙 null
     */
    public static String getString(String json, String path) {
        if (json == null || path == null) {
            return null;
        }
        try {
            Object value = JSONUtil.getByPath(JSONUtil.parse(json), path);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.debug("[JsonUtil] 茅聬聽盲戮炉氓聻掳莽禄露莽聜潞氓麓拢茅聫聧脩聙氓聛聯茅聬聯氓潞聸盲潞录茅聬聽?| path: {} | Error: {}", path, e.getMessage());
            return null;
        }
    }

    /**
     * 忙拢拢氓聠篓忙卤虏 getString 茅聳禄茫聞楼氓芦颅茅聫聠茂陆聣氓录芦茅聨碌卯聠录卯聲录茅聳潞?     */
    public static Integer getInt(String json, String path) {
        String val = getString(json, path);
        if (val == null || val.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 忙拢拢氓聠篓忙卤虏 getString 茅聳禄茫聞楼氓芦掳茅聫聠茅聛聧氓录芦莽聙聸忙篓禄忙庐露茅聳禄忙楼聙莽聣聤氓漏聙?     */
    public static Long getLong(String json, String path) {
        String val = getString(json, path);
        if (val == null || val.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 忙拢拢氓聠篓忙卤虏 getString 茅聳禄茫聞楼氓芦卢莽禄聰茅拢聨盲潞赂茅聫聝氓聜職卯聲录茅聳潞?     */
    public static Boolean getBoolean(String json, String path) {
        String val = getString(json, path);
        if (val == null || val.isEmpty()) {
            return null;
        }
        return Boolean.parseBoolean(val);
    }

    /**
     * 忙拢拢氓聠篓忙卤虏 茅聳赂忙聨聥氓聙聳茅聫聦氓聸漏芒聜卢忙露聶卯職聢茅隆聭盲陆鹿莽篓聣茅聧聶氓陇聥脨娄茅聳赂忙掳戮莽录職莽聮聬莽聠录氓麓楼茅聧芦莽聠潞茫聙聤 JSON
     *
     * @param str 氓篓麓莽聤碌莽虏炉茅聧聯忙聣庐芒聜卢忙露聶卯職聢茅隆聭盲陆鹿莽篓聣?     * @return true=茅聳潞氓聥碌氓聻掳茅聨庐氓潞隆芒聳聰茅聫聝莽赂聨ON茅聳驴忙露聵莽聲潞alse=氓篓聭忙聮鲁莽搂碌氓娄虏?     */
    public static boolean isValidJSON(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            JSONUtil.parse(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== 氓娄陇氓聜聸卯聼聡忙楼聽氓聸卢氓录卢莽聭聶氓聥颅茫聙聤茅聳驴忙露聶莽聯聤ode 茅聳鹿氓聻庐莽搂鲁莽录聧忙聴聢忙聲聧茅聧芦茫聢拢卯聺娄氓篓麓?JsonPath茅聳驴?===================

    /**
     * 忙拢拢氓聠篓忙卤虏 茅聳录忙聝搂氓聵虏猫陇掳?Jackson JsonNode茅聳驴忙露聵莽聣聤茅聫聛卯聡聙氓鹿聬忙碌拢氓聣虏忙搂禄茅聳潞氓陇聤氓聙鹿茅聨录茅聰聥忙聥聟氓漏聤忙聞庐莽陇聣氓漏碌芒聙鲁氓聙鹿茅聫聠莽聠潞莽麓聮茅聧聲卯聜拢氓聙陇忙楼聽莽聜露莽驴掳茅聢搂卯聝驴莽陇聛茅聬隆脩聝芒聳聯茅聧篓卯聜拢莽娄聦茅聳鹿猫炉隆忙戮聛莽禄卤?     *
     * 茅聳碌氓聴聵氓聤聮忙驴聻氓聸卢忙聜陆茅聤聤茫聝娄莽掳職茅聳潞氓聮聛氓聻卢茅聢搂?     * - 茅聴聜氓聸拢氓聛聯茅聬聼忙聸職莽陇聛茅隆聲?JSON 茅聳赂氓卢芦莽聼庐莽禄篓芒聲聛芒聳聨茅聢楼氓聫聣氓聫聫茅聳潞芒聜卢茅聬搂氓聯楼莽陇聞氓漏碌芒聙鲁氓聙禄莽禄庐莽聣聢忙陆禄茅聫聜忙聦聨莽陇聙茅聬聨莽聰碌卯聰聤茅聮聳氓聥炉氓麓聺茅聬聵猫戮漏卯聛卤氓篓聭忙聯聝盲潞聺茅聫聦氓漏聞芒聜卢忙露聶卯職聠茅隆聰氓虏聞忙聲聧?     * - 茅聴聜氓聸拢氓聛聯茅聬聼忙聸職莽陇聛茅聧篓盲禄聥氓录卢茅隆聯莽聜碌忙聭聼忙驴聻氓聽聺莽聛職氓娄虏忙聬聛氓麓楼茅聰聲芒聜卢茅聬隆茫聞漏氓麓路茅聤聤茂陆聜氓聛聯忙碌拢茅赂驴卯聺娄茅聳赂茫聞楼卯聝聠氓娄虏氓聭聤莽娄聮茅聢搂卯聞聛莽篓聤?     * - 茅聴聜氓聸拢氓聛聯茅聬聼忙聸職莽路職忙碌聹氓聽聲氓麓垄茅聧隆忙篓禄忙庐露莽录聜盲陆赂氓芦卢忙碌聽忙露聶忙陆禄茅聧楼茫聞娄氓搂垄
     *
     * @param json JSON 茅聬聨忙露聶卯職聢茅隆聭盲陆鹿莽篓聣?     * @return JsonNode茅聳驴忙露聵莽聣聫莽聬職卯聜拢氓炉录?DOM 茅聳潞氓露聠氓聻鹿氓篓聢忙聢娄氓录陆莽潞颅氓聭颅脦聺茅聳禄忙聞庐忙芦垄莽禄卤?     */
    public static JsonNode getNode(String json) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            log.error("[JsonUtil] 茅聬聼忙卢聶莽碌戮茅聬聨?JSON 氓漏垄忙聝掳莽虏戮莽聭聶?| Error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 忙拢拢氓聠篓忙卤虏 茅聳赂忙聨聥氓聙聳茅聫聦?JsonNode 茅聴聛忙聫聮忙聹卢茅聬聯氓聸篓莽篓聣茅隆聫氓聸漏莽聠聟莽聙碌忙聺驴氓芦颅氓娄虏忙聬聛氓麓楼茅聰聲芒聜卢茅聬隆茫聞漏氓麓路?     */
    public static boolean hasPath(JsonNode node, String path) {
        if (node == null || path == null) {
            return false;
        }
        String[] parts = path.split("\\.");
        JsonNode current = node;
        for (String part : parts) {
            if (current == null || !current.has(part)) {
                return false;
            }
            current = current.get(part);
        }
        return true;
    }

    /**
     * 忙拢拢氓聠篓忙卤虏 氓篓麓?Object茅聳驴忙露聵莽聣聠猫陇掳忙聼楼忙聡聴茅聭潞茫聝娄脨娄 Map/POJO/JSON茅聬聨忙露聶卯職聢茅隆聭盲陆鹿莽篓聣莽聮聡脩聟莽陇聠茅聬聨莽聭掳卯聵炉茅聧聶氓驴聸忙聡垄氓庐聞忙聺驴莽碌驴茅聳赂?     *
     * 茅聳碌氓聴聵氓聤聮忙驴聻氓聸卢忙聜陆茅聤聤茫聝娄莽掳職茅聳潞氓聮聛氓聻卢茅聢搂?     * - 茅聳鹿忙聛聮氓聞卤猫陇掳忙露垄氓麓拢茅聧聦忙露聶忙庐露莽聙鹿忙颅聦氓掳聶莽禄庐茂驴聽氓录掳茅隆聳忙卢聭卯聛卤氓篓聭忙聯聝盲潞聹茅隆聲卯聠戮忙聦聮茅聢陆氓聭炉莽陇聣氓篓聭忙聮鲁莽搂露茅聬聯茂驴聽忙聼聠茅聨戮氓聠虏氓戮聰氓篓麓茂陆聠忙聦戮莽聬職卯聜娄氓麓鹿?     * - 茅聳鹿卯聢職氓聲驴茅聬拢茫聞漏氓麓聴茅聤聤茫聞楼莽庐炉茅聳赂忙卢聬莽聣聴茅聬聯氓聸篓莽篓聣茅隆聫氓聥颅忙聭聼忙驴聻氓聽垄卯聲庐莽禄卤忙驴聡莽篓聣氓庐楼氓聴聴卯聺聰莽聙碌卯聠录氓聙赂茅聬聳?     *
     * @param obj 氓篓麓莽聤碌莽虏炉茅聧聯忙聣庐芒聜卢莽聰碌卯聰聤茅聮聳?     * @param fieldName 茅聬聨忙露聶卯職聠茅隆聰氓虏聞氓麓楼?     * @return 茅聬聨忙露聶卯職聠茅隆聰氓虏聞氓麓聬莽潞颅茅拢聨莽陇聣氓篓聭忙聮鲁莽搂麓茅聬隆茫聞漏氓麓路茅聤聤茫聞搂莽庐虏茅聳赂?Optional.empty()
     */
    public static Optional<Object> getField(Object obj, String fieldName) {
        if (obj == null || fieldName == null) {
            return Optional.empty();
        }
        try {
            if (obj instanceof String) {
                obj = JSONUtil.parse((String) obj);
            }
            if (obj instanceof JSONObject) {
                return Optional.ofNullable(((JSONObject) obj).get(fieldName));
            }
            JsonNode node = MAPPER.valueToTree(obj);
            JsonNode field = node.get(fieldName);
            return Optional.ofNullable(field != null ? field : null);
        } catch (Exception e) {
            log.debug("[JsonUtil] 茅聳录忙聝搂氓聵虏猫陇掳氓聸漏芒聜卢忙露聶卯職聠茅隆聰氓卤戮氓戮聞忙聺聢卯聜楼脨聲 | fieldName: {} | Error: {}", fieldName, e.getMessage());
            return Optional.empty();
        }
    }
}
