package com.zeljkobilbija001.motorwayinfo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Jedna saobraćajna poruka sa verkehr.autobahn.de (upozorenje ili zatvaranje).
 * Tekst opisa je na nemačkom, onako kako ga daje Autobahn GmbH; vrsta se određuje iz njega.
 */
public final class Dogadjaj {

    /** Vrste, poređane po važnosti (manji broj = važnije). */
    public static final int ZATVORENO = 0, UDES = 1, OPASNOST = 2, KOLONA = 3,
            TRAKA = 4, SPORO = 5, PRILAZ = 6, OSTALO = 7;

    public final String id;
    public final int vrsta;
    public final String deonica;   // "Pforzheim-West – Karlsruhe" (može biti prazno)
    public final String smer;      // "Stuttgart → Karlsruhe"
    public final String opis;      // nemački tekst, redovi spojeni sa \n
    public final int kasnjenjeMin; // 0 = nepoznato
    public final long pocetak;     // ms, 0 = nepoznato
    public final double lat, lon;  // početak; NaN ako nema
    public final double lat2, lon2; // kraj (iz geometrije, u smeru vožnje); NaN ako nema
    private boolean bezObavestenja;  // npr. vozilo u kvaru (Austrija): samo u listi

    private Dogadjaj(String id, int vrsta, String deonica, String smer, String opis,
                     int kasnjenjeMin, long pocetak, double lat, double lon, double lat2, double lon2) {
        this.id = id;
        this.vrsta = vrsta;
        this.deonica = deonica;
        this.smer = smer;
        this.opis = opis;
        this.kasnjenjeMin = kasnjenjeMin;
        this.pocetak = pocetak;
        this.lat = lat;
        this.lon = lon;
        this.lat2 = lat2;
        this.lon2 = lon2;
    }

    /** Da li zbog ove poruke vredi poslati obaveštenje (udes, kolona, zatvoren put, opasnost). */
    public boolean zaObavestenje() {
        if (bezObavestenja) return false;
        switch (vrsta) {
            case ZATVORENO:
            case UDES:
            case OPASNOST:
                return true;
            case KOLONA:
                return kasnjenjeMin == 0 || kasnjenjeMin >= 10;
            default:
                return false;
        }
    }

    public boolean imaKoordinate() {
        return !Double.isNaN(lat) && !Double.isNaN(lon);
    }

    // ---------- Velika Britanija: Traffic Wales i TrafficWatchNI (javni RSS, bez ključa) ----------

    private static final java.util.regex.Pattern RSS_ITEM = java.util.regex.Pattern.compile("<item>(.*?)</item>",
            java.util.regex.Pattern.DOTALL);

    private static String tag(String xml, String ime) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("<" + ime + "[^>]*>(.*?)</" + ime + ">",
                java.util.regex.Pattern.DOTALL).matcher(xml);
        if (!m.find()) return "";
        return m.group(1).replace("<![CDATA[", "").replace("]]>", "").replace("&amp;", "&")
                .replace("&#039;", "'").replace("&apos;", "'").replace("&quot;", "\"").trim();
    }

    /** "M4", "A1(M)" … iz oznake autoputa "GB-M4" (poređenje bez razmaka, velikim slovima). */
    private static boolean istiPut(String put, String ref) {
        return put.replace(" ", "").equalsIgnoreCase(ref.replace(" ", ""));
    }

    /**
     * Traffic Wales RSS (traffic.wales/feeds/…): naslov "M4 : Eastbound : J23A Magor : Onslip closed : Roadworks : …",
     * opis sa "Start time: 24/11/2026 20:00 , End Date:  27/11/2026 6:00, Severity: Road closure", tačka u georss:point.
     * Radovi (često samo noću) idu samo u listu; obaveštenja samo za incidente. Izvor se navodi: „Traffic Wales“.
     */
    static List<Dogadjaj> izWales(String xml, String ref, boolean radovi) {
        List<Dogadjaj> l = new ArrayList<>();
        long sada = System.currentTimeMillis();
        SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy H:mm", Locale.UK);
        f.setTimeZone(TimeZone.getTimeZone("Europe/London"));
        java.util.regex.Matcher m = RSS_ITEM.matcher(xml);
        while (m.find()) {
            String it = m.group(1), naslov = tag(it, "title"), opis = tag(it, "description");
            String[] d = naslov.split("\\s*:\\s*");
            if (d.length < 2 || !istiPut(d[0], ref)) continue;
            long od = 0, doK = 0;
            java.util.regex.Matcher t = java.util.regex.Pattern.compile("Start time:\\s*([0-9/]+ [0-9:]+)").matcher(opis);
            try { if (t.find()) od = f.parse(t.group(1)).getTime(); } catch (Exception ignored) { }
            t = java.util.regex.Pattern.compile("End Date:\\s*([0-9/]+ [0-9:]+)").matcher(opis);
            try { if (t.find()) doK = f.parse(t.group(1)).getTime(); } catch (Exception ignored) { }
            if (od > sada || (doK > 0 && doK < sada)) continue; // još nije počelo ili je prošlo
            String sve = naslov.toLowerCase(Locale.UK);
            boolean prilaz = sve.contains("slip") && !sve.matches(".*\\bclosed \\(between the slips\\).*");
            int vrsta;
            if (sve.contains("collision") || sve.contains("accident") || sve.contains("crash")) vrsta = UDES;
            else if (sve.contains("closed")) vrsta = prilaz ? PRILAZ : ZATVORENO;
            else if (sve.contains("congestion") || sve.contains("queu") || sve.contains("delays")) vrsta = KOLONA;
            else if (radovi || sve.contains("lane")) vrsta = TRAKA;
            else vrsta = OPASNOST;
            String smer = d[1], strana = smer.toLowerCase(Locale.UK);
            double lat = Double.NaN, lon = Double.NaN, lat2 = Double.NaN, lon2 = Double.NaN;
            String[] g = tag(it, "georss:point").split("\\s+");
            if (g.length == 2) {
                try {
                    lat = Double.parseDouble(g[0]);
                    lon = Double.parseDouble(g[1]);
                    // druga tačka ~1 km u smeru vožnje – Vožnja tako zna za koju stranu je poruka
                    double dLat = 0, dLon = 0, k = 0.009;
                    if (strana.startsWith("east")) dLon = k / Math.cos(Math.toRadians(lat));
                    else if (strana.startsWith("west")) dLon = -k / Math.cos(Math.toRadians(lat));
                    else if (strana.startsWith("north")) dLat = k;
                    else if (strana.startsWith("south")) dLat = -k;
                    if (dLat != 0 || dLon != 0) { lat2 = lat + dLat; lon2 = lon + dLon; }
                } catch (NumberFormatException ignored) { }
            }
            String link = tag(it, "link");
            Dogadjaj x = new Dogadjaj(tag(it, "guid"), vrsta, d.length > 2 ? d[2] : "",
                    strana.startsWith("both") ? "" : "→ " + smer,
                    naslov.replaceAll("\\s*:\\s*$", "") + (link.isEmpty() ? "" : "\n" + link), 0, od, lat, lon, lat2, lon2);
            x.bezObavestenja = radovi;
            l.add(x);
        }
        return l;
    }

    /**
     * TrafficWatchNI RSS (Severna Irska): naslov "M1 East Bound  , M1, Kilvergan Road, Craigavon", tekst u opisu,
     * bez koordinata (samo lista). Preskaču se završeni i stari događaji. Izvor: „DfI Traffic Information and Control Centre“.
     */
    static List<Dogadjaj> izNi(String xml, String ref) {
        List<Dogadjaj> l = new ArrayList<>();
        long sada = System.currentTimeMillis();
        SimpleDateFormat f = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
        java.util.regex.Matcher m = RSS_ITEM.matcher(xml);
        while (m.find()) {
            String it = m.group(1), naslov = tag(it, "title"), opis = tag(it, "description").replaceAll("\\s+", " ");
            boolean nasPut = false;
            for (String deo : naslov.split(",")) {
                String p = deo.trim();
                if (istiPut(p, ref) || p.toUpperCase(Locale.UK).startsWith(ref.toUpperCase(Locale.UK) + " ")) nasPut = true;
            }
            if (!nasPut) continue;
            String mala = opis.toLowerCase(Locale.UK);
            if (mala.contains("cleared") || mala.contains("reopened") || mala.contains("re-opened")) continue;
            long od = 0;
            try { od = f.parse(tag(it, "pubDate")).getTime(); } catch (Exception ignored) { }
            if (od > 0 && sada - od > 12 * 3600_000L) continue; // stari zapisi koji ostaju u feed-u
            int vrsta = mala.contains("collision") || mala.contains("crash") ? UDES
                    : mala.contains("closed") ? ZATVORENO
                    : mala.contains("queue") || mala.contains("congestion") || mala.contains("delays") ? KOLONA : OPASNOST;
            String[] d = naslov.split(",");
            String smer = d[0].replaceFirst("(?i)^" + java.util.regex.Pattern.quote(ref), "").trim();
            l.add(new Dogadjaj(tag(it, "guid"), vrsta, d.length > 2 ? d[2].trim() : "",
                    smer.isEmpty() ? "" : "→ " + smer, opis, 0, od,
                    Double.NaN, Double.NaN, Double.NaN, Double.NaN));
        }
        return l;
    }

    // ---------- Austrija: EVIS.AT / ASFINAG preko našeg posrednika (Cloudflare Worker) ----------

    /**
     * Odgovor posrednika evis-posrednik.zeljkobilbija.workers.dev: {"dogadjaji":[{id, p, t, d, st, en, o, m, s, a, b, man}]}
     * (t = vrste zapisa DATEX II: Accident, AbnormalTraffic, RoadOrCarriagewayOrLaneManagement…; d = podvrste).
     */
    static List<Dogadjaj> izEvis(String json) throws Exception {
        List<Dogadjaj> l = new ArrayList<>();
        JSONArray a = new JSONObject(json).optJSONArray("dogadjaji");
        if (a == null) return l;
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o == null) continue;
            JSONArray t = o.optJSONArray("t"), d = o.optJSONArray("d");
            String tipovi = t == null ? "" : t.toString(), podtipovi = d == null ? "" : d.toString();
            int vrsta = vrstaEvis(tipovi, podtipovi);
            String opis = o.optString("o", "");
            String deonica = mestoEvis(opis);
            if (deonica.isEmpty()) deonica = o.optString("m", "");
            String smer = o.optString("s", "");
            JSONArray p1 = o.optJSONArray("a"), p2 = o.optJSONArray("b");
            Dogadjaj x = new Dogadjaj(o.optString("id"), vrsta, deonica, smer.isEmpty() ? "" : "→ " + smer, opis, 0,
                    o.optLong("st", 0),
                    p1 != null ? p1.optDouble(0) : Double.NaN, p1 != null ? p1.optDouble(1) : Double.NaN,
                    p2 != null ? p2.optDouble(0) : Double.NaN, p2 != null ? p2.optDouble(1) : Double.NaN);
            x.bezObavestenja = tipovi.contains("VehicleObstruction") && podtipovi.contains("brokenDownVehicle")
                    && !tipovi.contains("Accident");
            l.add(x);
        }
        return l;
    }

    // Planirani radovi: "A1 West Autobahn: In Fahrtrichtung von Linz nach Wien zwischen Anschlussstelle Haag und
    // Anschlussstelle St.Valentin sind …, 16.11.2026 - 11.12.2026: Mo, Di …" (ranije je naslov bio „Di“).
    private static final java.util.regex.Pattern EVIS_PLAN = java.util.regex.Pattern.compile(
            "[Ii]n Fahrtrichtung .*? (?:zwischen|bei) (.+?)(?: ist | sind | wird | Verkehr | veränderte | gesperrt| Die |,|$)");

    /** Mesto (deonica) iz opisa EVIS vesti, bez kilometara; prazno ako se ne prepozna. */
    static String mestoEvis(String opis) {
        String m = "";
        java.util.regex.Matcher pl = EVIS_PLAN.matcher(opis);
        if (pl.find()) {
            m = pl.group(1);
        } else {
            // Uživo: "A9 Pyhrn Autobahn, Graz Richtung Maribor (SLO), zwischen Übelbach (km 157.1) und Peggau (km 165.2), …"
            String[] delovi = opis.split(", ");
            if (delovi.length >= 3) m = delovi[2];
        }
        return m.replaceFirst("^zwischen ", "").replace("Anschlussstelle ", "").replace(" und ", " – ")
                .replaceAll(" \\(km [\\d.,]+\\)", "").trim();
    }

    static int vrstaEvis(String t, String d) {
        if (t.contains("Accident")) return UDES;
        if (t.contains("RoadOrCarriagewayOrLaneManagement") && (d.contains("roadClosed") || d.contains("carriagewayClosures"))) {
            return d.contains("slipRoads") && !d.contains("mainCarriageway") ? PRILAZ : ZATVORENO;
        }
        if (t.contains("AbnormalTraffic")) return d.contains("slowTraffic") || d.contains("heavyTraffic") ? SPORO : KOLONA;
        if (t.contains("Obstruction") || t.contains("PoorEnvironment") || t.contains("WeatherRelated")
                || t.contains("NonWeatherRelated")) return OPASNOST;
        if (t.contains("RoadOrCarriagewayOrLaneManagement") || t.contains("MaintenanceWorks")
                || t.contains("ConstructionWorks")) return TRAKA;
        if (t.contains("SpeedManagement")) return SPORO;
        return OSTALO;
    }

    // ---------- Čitanje JSON-a ----------

    /** Lista iz odgovora servisa "warning" ili "closure"; najavljena (buduća) zatvaranja se preskaču. */
    static List<Dogadjaj> izJsona(String json, String kljuc) throws Exception {
        List<Dogadjaj> l = new ArrayList<>();
        JSONArray a = new JSONObject(json).optJSONArray(kljuc);
        if (a == null) return l;
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o == null || o.optBoolean("future", false)) continue;
            Dogadjaj d = jedan(o);
            if (d != null) l.add(d);
        }
        return l;
    }

    private static Dogadjaj jedan(JSONObject o) {
        String id = stabilanId(o.optString("identifier", ""));
        if (id.isEmpty()) return null;

        StringBuilder sb = new StringBuilder();
        JSONArray opisi = o.optJSONArray("description");
        if (opisi != null) {
            boolean prazan = true;
            for (int i = 0; i < opisi.length(); i++) {
                String red = opisi.optString(i, "").trim();
                if (red.isEmpty()) {
                    prazan = true;
                    continue;
                }
                if (sb.length() > 0) sb.append(prazan ? "\n\n" : "\n");
                sb.append(red);
                prazan = false;
            }
        }
        String opis = sb.toString();

        int kasnjenje = 0;
        try {
            kasnjenje = Integer.parseInt(o.optString("delayTimeValue", "0").trim());
        } catch (NumberFormatException ignored) { }

        String prikaz = o.optString("display_type", "");
        int vrsta;
        if ("CLOSURE".equals(prikaz)) vrsta = ZATVORENO;
        else if ("CLOSURE_ENTRY_EXIT".equals(prikaz)) vrsta = PRILAZ;
        else vrsta = vrstaUpozorenja(opis.toLowerCase(Locale.GERMAN),
                    o.optString("abnormalTrafficType", ""), "true".equals(o.optString("isBlocked")), kasnjenje);

        String naslov = o.optString("title", "");
        int crta = naslov.indexOf('|');
        String deonica = crta >= 0 ? naslov.substring(crta + 1).trim() : "";
        if ("Verkehrsmeldung".equalsIgnoreCase(deonica)) deonica = "";
        deonica = deonica.replace(" - ", " – ");

        String smer = o.optString("subtitle", "").trim().replace("->", "→");

        double lat = Double.NaN, lon = Double.NaN;
        JSONObject k = o.optJSONObject("coordinate");
        if (k != null) {
            lat = k.optDouble("lat", Double.NaN);
            lon = k.optDouble("long", Double.NaN);
        }
        // kraj poruke: poslednja tačka linije (GeoJSON: [lon, lat])
        double lat2 = Double.NaN, lon2 = Double.NaN;
        JSONObject g = o.optJSONObject("geometry");
        JSONArray tacke = g != null ? g.optJSONArray("coordinates") : null;
        if (tacke != null && tacke.length() > 1) {
            JSONArray z = tacke.optJSONArray(tacke.length() - 1);
            if (z != null && z.length() >= 2) {
                lon2 = z.optDouble(0, Double.NaN);
                lat2 = z.optDouble(1, Double.NaN);
            }
        }

        return new Dogadjaj(id, vrsta, deonica, smer, opis, kasnjenje,
                vreme(o.optString("startTimestamp", "")), lat, lon, lat2, lon2);
    }

    /**
     * Oznaka koja se ne menja dok poruka traje. Zvanične poruke ("NLW_2026_002954--vi-hind.<vreme>",
     * zatvaranja "2026-058313--…") posle "--" dobijaju vreme poslednje izmene, pa se uzima deo pre "--"
     * (broj događaja). INRIX poruke ("INRIX--vi-avl.<početak>_NNN") su stalne u celini.
     */
    static String stabilanId(String id) {
        int i = id.indexOf("--");
        if (i <= 0) return id;
        String pre = id.substring(0, i);
        return pre.matches(".*\\d.*") ? pre : id;
    }

    private static int vrstaUpozorenja(String t, String tip, boolean blokirano, int kasnjenje) {
        if (t.contains("falschfahrer")) return OPASNOST;
        if (t.contains("unfall")) return UDES;
        if (blokirano || t.contains("vollsperrung") || t.contains("fahrbahn gesperrt")
                || t.contains("gesperrt in beide")) return ZATVORENO;
        if (t.contains("gegenstand") || t.contains("brand") || t.contains("glätte") || t.contains("glatteis")
                || t.contains("nebel") || t.contains("tiere") || t.contains("personen auf der fahrbahn")
                || t.contains("fahrbahnschäden") || t.contains("liegengeblieben") || t.contains("panne")
                || t.contains("gefahr")) return OPASNOST;
        if ("QUEUING_TRAFFIC".equals(tip) || t.contains("stau")) return KOLONA;
        if (t.contains("fahrstreifen gesperrt") || t.contains("fahrstreifen blockiert")) return TRAKA;
        if ("SLOW_TRAFFIC".equals(tip) || "HEAVY_TRAFFIC".equals(tip) || "UNSPECIFIED_ABNORMAL_TRAFFIC".equals(tip)
                || t.contains("stockend") || t.contains("durchschnittsgeschwindigkeit")) {
            return kasnjenje >= 15 ? KOLONA : SPORO;
        }
        return OSTALO;
    }

    private static long vreme(String iso) {
        if (iso.length() < 19) return 0;
        try {
            SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            f.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date d = f.parse(iso.substring(0, 19));
            return d == null ? 0 : d.getTime();
        } catch (Exception e) {
            return 0;
        }
    }
}
