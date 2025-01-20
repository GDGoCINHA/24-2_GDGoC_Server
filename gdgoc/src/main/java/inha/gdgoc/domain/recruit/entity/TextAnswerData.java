package inha.gdgoc.domain.recruit.entity;

public class TextAnswerData extends AnswerData {
    private String text;

    @Override
    public void validate() {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text answer cannot be empty");
        }
    }
}
