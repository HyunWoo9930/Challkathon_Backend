package challkahthon.backend.hihigh.jwt;

public interface OAuth2UserInfo {
	String getProvider();
	String getProviderId();
	String getEmail();
	String getName();
}