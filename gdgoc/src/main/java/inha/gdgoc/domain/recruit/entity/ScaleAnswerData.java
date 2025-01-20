package inha.gdgoc.domain.recruit.entity;

public class ScaleAnswerData extends AnswerData {
    private int value;
    private int minValue;
    private int maxValue;

    @Override
    public void validate() {
        if (value < minValue || value > maxValue) {
            throw new IllegalArgumentException(
                    String.format("Value must be between %d and %d", minValue, maxValue)
            );
        }
    }
}