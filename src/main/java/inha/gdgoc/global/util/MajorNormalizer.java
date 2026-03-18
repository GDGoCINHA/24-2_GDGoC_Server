package inha.gdgoc.global.util;

import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class MajorNormalizer {

    private static final Map<String, String> LABEL_TO_CODE = Map.ofEntries(
            Map.entry("인공지능공학과", "AIE"),
            Map.entry("데이터사이언스학과", "DSE"),
            Map.entry("스마트모빌리티공학과", "SME"),
            Map.entry("디자인테크놀로지학과", "DTE"),
            Map.entry("컴퓨터공학과", "CSE"),
            Map.entry("기계공학과", "ME"),
            Map.entry("항공우주공학과", "AAE"),
            Map.entry("조선해양공학과", "NAE"),
            Map.entry("산업경영공학과", "IME"),
            Map.entry("화학공학과", "CHE"),
            Map.entry("고분자공학과", "PSE"),
            Map.entry("신소재공학과", "MSE"),
            Map.entry("사회인프라공학과", "CIE"),
            Map.entry("환경공학과", "ENVE"),
            Map.entry("공간정보공학과", "GIE"),
            Map.entry("건축학부(건축공학)", "ACE"),
            Map.entry("건축학부(건축학)", "ARCH"),
            Map.entry("에너지자원공학과", "ERE"),
            Map.entry("융합기술경영학부", "MOT"),
            Map.entry("전기전자공학부", "EEE"),
            Map.entry("반도체시스템공학과", "SSE"),
            Map.entry("이차전지융합학과", "BCE"),
            Map.entry("수학과", "MATH"),
            Map.entry("통계학과", "STAT"),
            Map.entry("물리학과", "PHYS"),
            Map.entry("화학과", "CHEM"),
            Map.entry("해양과학과", "OCS"),
            Map.entry("식품영양학과", "FNS"),
            Map.entry("경영학부(경영학과)", "BUS"),
            Map.entry("경영학부(파이낸스경영학과)", "FIN"),
            Map.entry("아태물류학부", "APL"),
            Map.entry("국제통상학과", "ITC"),
            Map.entry("조형예술학과", "FINEART"),
            Map.entry("디자인융합학과", "ID"),
            Map.entry("스포츠과학과", "SPORTS"),
            Map.entry("연극영화학과", "TFA"),
            Map.entry("의류디자인학과", "FD"),
            Map.entry("행정학과", "PAD"),
            Map.entry("정치외교학과", "POL"),
            Map.entry("미디어커뮤니케이션학과", "MCS"),
            Map.entry("경제학과", "ECON"),
            Map.entry("소비자학과", "CONS"),
            Map.entry("아동심리학과", "CPSY"),
            Map.entry("사회복지학과", "SW"),
            Map.entry("자유전공융합학부", "ULS"),
            Map.entry("공학융합학부", "ECS"),
            Map.entry("자연과학융합학부", "NCS"),
            Map.entry("경영융합학부", "BCONV"),
            Map.entry("사회과학융합학부", "SCS"),
            Map.entry("인문융합학부", "HCS"),
            Map.entry("한국어문학과", "KLL"),
            Map.entry("사학과", "HIST"),
            Map.entry("철학과", "PHIL"),
            Map.entry("중국학과", "CHIN"),
            Map.entry("일본언어문화학과", "JLC"),
            Map.entry("영미유럽인문융합학부", "ELH"),
            Map.entry("문화콘텐츠문화경영학과", "CCM"),
            Map.entry("메카트로닉스공학과", "MTE"),
            Map.entry("소프트웨어융합공학과", "SWE"),
            Map.entry("산업경영학과", "IMGT"),
            Map.entry("금융투자학과", "FI"),
            Map.entry("생명공학과", "BIOE"),
            Map.entry("바이오제약공학과", "BPE"),
            Map.entry("생명과학과", "BIOS"),
            Map.entry("첨단바이오의약학과", "ABM"),
            Map.entry("바이오식품공학과", "BFE"),
            Map.entry("IBT학과", "IBT"),
            Map.entry("ISE학과", "ISE"),
            Map.entry("KLC학과", "KLC"),
            Map.entry("의예과", "PREMED"),
            Map.entry("의학과", "MED"),
            Map.entry("간호학과", "NURS"),
            Map.entry("국어교육과", "KOR_EDU"),
            Map.entry("영어교육과", "ENG_EDU"),
            Map.entry("사회교육과", "SOC_EDU"),
            Map.entry("교육학과", "EDU"),
            Map.entry("체육교육과", "PE_EDU"),
            Map.entry("수학교육과", "MATH_EDU")
    );

    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("스마트모빌리티놀학과", "SME"),
            Map.entry("건축공학전공", "ACE"),
            Map.entry("건축학전공(5년제)", "ARCH"),
            Map.entry("경영학과", "BUS"),
            Map.entry("파이낸스경영학과", "FIN"),
            Map.entry("파이낸스경영학부(경영학과)", "FIN"),
            Map.entry("문화콘텐츠문화경영학부(경영학과)", "CCM"),
            Map.entry("문화컨텐츠경영학과", "CCM")
    );

    private static final Set<String> KNOWN_CODES = Set.copyOf(LABEL_TO_CODE.values());

    public String normalize(String major) {
        if (major == null) {
            return null;
        }

        String trimmed = major.trim();
        if (trimmed.isBlank()) {
            return trimmed;
        }

        if (KNOWN_CODES.contains(trimmed)) {
            return trimmed;
        }

        String fromLabel = LABEL_TO_CODE.get(trimmed);
        if (fromLabel != null) {
            return fromLabel;
        }

        return ALIASES.getOrDefault(trimmed, trimmed);
    }
}
