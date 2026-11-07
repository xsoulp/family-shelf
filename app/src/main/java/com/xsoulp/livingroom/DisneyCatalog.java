package com.xsoulp.livingroom;

/** Fixed photo order; release years prevent remakes and sequels taking a slot. */
final class DisneyCatalog {
    static final String[] TITLES={"The Little Mermaid","Sleeping Beauty","Beauty and the Beast","Brave","The Princess and the Frog","Mulan",
        "Snow White and the Seven Dwarfs","Cinderella","Aladdin","Moana","Pocahontas","Tangled"};
    static final int[] YEARS={1989,1959,1991,2012,2009,1998,1937,1950,1992,2016,1995,2010};
    private static final String[][] ALIASES={
        {"A Pequena Sereia","The Little Mermaid"}, {"A Bela Adormecida","Sleeping Beauty"},
        {"A Bela e o Monstro","A Bela e a Fera","Beauty and the Beast"}, {"Brave","Brave Indomável","Indomável","Valente"},
        {"A Princesa e o Sapo","The Princess and the Frog"}, {"Mulan"},
        {"Branca de Neve e os Sete Anões","Snow White and the Seven Dwarfs"}, {"Cinderela","Cinderella"},
        {"Aladdin","Aladino"}, {"Vaiana","Moana"}, {"Pocahontas"}, {"Entrelaçados","Enrolados","Tangled"}
    };
    static String normalize(String title) {
        return java.text.Normalizer.normalize(title,java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{M}","").toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]","");
    }
    static int slot(String name,String original,int year) {
        String normalized=normalize(name), originalNormalized=normalize(original);
        // Confirmed library entry: this filename has no original title or release year.
        if(normalized.equals(normalize("A Bela Adormecida.ia")) && (year==0 || year==1959)) return 1;
        for(int i=0;i<TITLES.length;i++) {
            // Jellyfin may date the same Snow White animation to its 1938 release.
            if(year!=YEARS[i] && !(i==6 && year==1938)) continue;
            for(String alias:ALIASES[i]) if(normalize(alias).equals(normalized)||normalize(alias).equals(originalNormalized)) return i;
        }
        return -1;
    }
}
