import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

// --- MODELLER (MODELS) ---

/**
 * Islem Sınıfı: Her bir alım-satım hareketini temsil eder.
 */
class Islem {
    private final String tip;
    private final String hisseKod;
    private final int adet;
    private final double fiyat;
    private final LocalDateTime zaman;

    public Islem(String tip, String hisseKod, int adet, double fiyat) {
        this.tip = tip;
        this.hisseKod = hisseKod;
        this.adet = adet;
        this.fiyat = fiyat;
        this.zaman = LocalDateTime.now();
    }

    @Override
    public String toString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        return String.format("[%s] %-5s | %s x%d | Birim Fiyat: %.2f TL", 
                zaman.format(formatter), tip, hisseKod, adet, fiyat);
    }
}

class Hisse {
    private final String kod;
    private final String ad;
    private double fiyat;

    public Hisse(String kod, String ad, double fiyat) {
        this.kod = kod;
        this.ad = ad;
        this.fiyat = fiyat;
    }

    public String getKod() { return kod; }
    public double getFiyat() { return fiyat; }
    public void setFiyat(double fiyat) { this.fiyat = fiyat; }

    @Override
    public String toString() {
        return String.format("[%s] %-15s | Fiyat: %.2f TL", kod, ad, fiyat);
    }
}

class Kullanici {
    private final String isim;
    private double bakiye;
    private final Map<String, Integer> portfoy;
    private final List<Islem> islemGecmisi;

    public Kullanici(String isim, double baslangicBakiyesi) {
        this.isim = isim;
        this.bakiye = baslangicBakiyesi;
        this.portfoy = new HashMap<>();
        this.islemGecmisi = new ArrayList<>();
    }

    public String getIsim() { return isim; }
    public double getBakiye() { return bakiye; }
    public void setBakiye(double bakiye) { this.bakiye = bakiye; }
    public Map<String, Integer> getPortfoy() { return portfoy; }
    public List<Islem> getIslemGecmisi() { return islemGecmisi; }
    
    public void bakiyeGuncelle(double miktar) { this.bakiye += miktar; }
    public void gecmiseEkle(Islem islem) { islemGecmisi.add(islem); }

    public void portfoyeEkle(String hisseKod, int adet) {
        portfoy.put(hisseKod, portfoy.getOrDefault(hisseKod, 0) + adet);
    }

    public boolean portfoydenCikar(String hisseKod, int adet) {
        if (portfoy.getOrDefault(hisseKod, 0) >= adet) {
            portfoy.put(hisseKod, portfoy.get(hisseKod) - adet);
            if (portfoy.get(hisseKod) == 0) portfoy.remove(hisseKod);
            return true;
        }
        return false;
    }
}

// --- DOSYA SERVİSİ (PERSISTENCE) ---

class DosyaServisi {
    private static final String DOSYA_ADI = "borsa_verileri.txt";

    public static void kaydet(Kullanici user) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(DOSYA_ADI))) {
            writer.println(user.getBakiye());
            StringBuilder sb = new StringBuilder();
            user.getPortfoy().forEach((k, v) -> sb.append(k).append(":").append(v).append(","));
            writer.println(sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "EMPTY");
            System.out.println("Sistem: Veriler kaydedildi.");
        } catch (IOException e) {
            System.out.println("Hata: Kayıt başarısız!");
        }
    }

    public static void yukle(Kullanici user) {
        File f = new File(DOSYA_ADI);
        if (!f.exists()) return;
        try (Scanner fs = new Scanner(f)) {
            if (fs.hasNextLine()) user.setBakiye(Double.parseDouble(fs.nextLine()));
            if (fs.hasNextLine()) {
                String line = fs.nextLine();
                if (!line.equals("EMPTY")) {
                    for (String p : line.split(",")) {
                        String[] d = p.split(":");
                        user.portfoyeEkle(d[0], Integer.parseInt(d[1]));
                    }
                }
            }
            System.out.println("Sistem: Önceki oturum yüklendi.");
        } catch (Exception e) {
            System.out.println("Sistem: Yeni profil oluşturuldu.");
        }
    }
}

// --- SERVİSLER ---

class BorsaServisi {
    private final List<Hisse> marketHisseleri = new ArrayList<>();
    private final Random random = new Random();

    public BorsaServisi() {
        marketHisseleri.add(new Hisse("THYAO", "Türk Hava Yolları", 285.50));
        marketHisseleri.add(new Hisse("ASELS", "Aselsan", 58.20));
        marketHisseleri.add(new Hisse("EREGL", "Erdemir", 42.10));
        marketHisseleri.add(new Hisse("SASANI", "Sasa Polyester", 38.45));
    }

    public void fiyatlariGuncelle() {
        marketHisseleri.forEach(h -> h.setFiyat(h.getFiyat() * (0.95 + 0.1 * random.nextDouble())));
    }

    public Hisse hisseBul(String kod) {
        return marketHisseleri.stream().filter(h -> h.getKod().equalsIgnoreCase(kod)).findFirst().orElse(null);
    }

    public void hisseSatinAl(Kullanici user, String kod, int adet) {
        Hisse h = hisseBul(kod);
        if (h != null && user.getBakiye() >= h.getFiyat() * adet) {
            user.bakiyeGuncelle(-(h.getFiyat() * adet));
            user.portfoyeEkle(kod.toUpperCase(), adet);
            user.gecmiseEkle(new Islem("ALIS", kod.toUpperCase(), adet, h.getFiyat()));
            System.out.println("İşlem başarılı.");
        } else System.out.println("Hata: Yetersiz bakiye veya hatalı kod!");
    }

    public void hisseSat(Kullanici user, String kod, int adet) {
        Hisse h = hisseBul(kod);
        if (h != null && user.portfoydenCikar(kod.toUpperCase(), adet)) {
            user.bakiyeGuncelle(h.getFiyat() * adet);
            user.gecmiseEkle(new Islem("SATIS", kod.toUpperCase(), adet, h.getFiyat()));
            System.out.println("Satış başarılı.");
        } else System.out.println("Hata: Portföy yetersiz!");
    }

    public List<Hisse> getHisseler() { return marketHisseleri; }
}

// --- ANA UYGULAMA ---

public class Main {
    private static final Scanner sc = new Scanner(System.in);
    private static final BorsaServisi borsa = new BorsaServisi();
    private static final Kullanici kullanici = new Kullanici("Muhammed", 10000.0);

    public static void main(String[] args) {
        DosyaServisi.yukle(kullanici);
        System.out.println("📈 Hoş geldin, " + kullanici.getIsim() + "!");
        
        boolean aktif = true;
        while (aktif) {
            System.out.println("\nBakiye: " + String.format("%.2f TL", kullanici.getBakiye()));
            System.out.print("1-Piyasa 2-Al 3-Sat 4-Portföy 5-Geçmiş 6-Zamanı İlerlet 7-Kaydet ve Çık\nSeçim: ");
            
            switch (sc.nextLine()) {
                case "1" -> borsa.getHisseler().forEach(System.out::println);
                case "2" -> islemYap(true);
                case "3" -> islemYap(false);
                case "4" -> portfoyGoster();
                case "5" -> {
                    if (kullanici.getIslemGecmisi().isEmpty()) System.out.println("Geçmiş boş.");
                    else kullanici.getIslemGecmisi().forEach(System.out::println);
                }
                case "6" -> { borsa.fiyatlariGuncelle(); System.out.println("Fiyatlar güncellendi!"); }
                case "7" -> { DosyaServisi.kaydet(kullanici); aktif = false; }
                default -> System.out.println("Geçersiz seçim!");
            }
        }
    }

    private static void islemYap(boolean satinAl) {
        System.out.print("Kod: ");
        String kod = sc.nextLine();
        System.out.print("Adet: ");
        try {
            int adet = Integer.parseInt(sc.nextLine());
            if (satinAl) borsa.hisseSatinAl(kullanici, kod, adet);
            else borsa.hisseSat(kullanici, kod, adet);
        } catch (NumberFormatException e) {
            System.out.println("Hata: Lütfen sayısal bir değer giriniz!");
        }
    }

    private static void portfoyGoster() {
        if (kullanici.getPortfoy().isEmpty()) System.out.println("Portföyünüz boş.");
        else kullanici.getPortfoy().forEach((k, v) -> {
            Hisse h = borsa.hisseBul(k);
            double d = (h != null) ? h.getFiyat() * v : 0;
            System.out.println(k + " x" + v + " | Değer: " + String.format("%.2f TL", d));
        });
    }
}