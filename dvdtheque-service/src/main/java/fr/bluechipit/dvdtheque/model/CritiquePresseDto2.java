package fr.bluechipit.dvdtheque.model;

public record CritiquePresseDto2(String newsSource,
                                 Double rating,
                                 String body,
                                 String author) {

    public static CritiquePresseDto2 of(CritiquePresse critiquePresse) {
        if (critiquePresse == null) {
            return null;
        }
        return new CritiquePresseDto2(
                critiquePresse.getNewsSource(),
                critiquePresse.getRating(),
                critiquePresse.getBody(),
                critiquePresse.getAuthor()
        );
    }

    public static java.util.List<CritiquePresseDto2> of(java.util.List<CritiquePresse> critiquesPresse) {
        if (critiquesPresse == null) {
            return null;
        }
        java.util.List<CritiquePresseDto2> dtoList = new java.util.ArrayList<>();
        for (CritiquePresse critique : critiquesPresse) {
            dtoList.add(of(critique));
        }
        return dtoList;
    }
}
