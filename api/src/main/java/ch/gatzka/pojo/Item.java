package ch.gatzka.pojo;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
public class Item {

  private String uid;

  private String name;

  private Boolean bannedOnFlea;

  private Boolean haveMarketData;

  private String shortName;

  private Integer price;

  private Integer basePrice;

  private Integer avg24hPrice;

  private Integer avg7daysPrice;

  private String traderName;

  private Integer traderPrice;

  private String traderPriceCur;

  private Integer traderPriceRub;

  private String updated;

  private Integer slots;

  private String icon;

  private String link;

  private String wikiLink;

  private String img;

  private String imgBig;

  private String bsgId;

  private String[] tags;

  private Double diff24h;

  private Double diff7days;

  private Boolean isFunctional;

  private String reference;

}