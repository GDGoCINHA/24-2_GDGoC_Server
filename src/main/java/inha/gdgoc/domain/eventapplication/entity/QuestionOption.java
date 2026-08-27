package inha.gdgoc.domain.eventapplication.entity;

/**
 * 선택형 질문의 선택지.
 *
 * <p>답변에는 {@code value} 를 저장하고 화면에는 {@code label} 을 보여준다. 그래야 운영진이 나중에 문구를 고쳐도 기존 답변이 깨지지 않는다. 라벨을
 * 그대로 저장하면 오타 하나 고칠 때마다 과거 답변이 고아가 된다.
 */
public record QuestionOption(String value, String label) {}
