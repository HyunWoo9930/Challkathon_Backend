package challkahthon.backend.hihigh.jwt.google;

import java.util.Map;

import challkahthon.backend.hihigh.jwt.OAuth2UserInfo;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class GoogleUserDetails implements OAuth2UserInfo {

	private Map<String, Object> attributes;

	@Override
	public String getProvider() {
		return "google";
	}

	@Override
	public String getProviderId() {
		return (String) attributes.get("sub");
	}

	@Override
	public String getEmail() {
		return (String) attributes.get("email");
	}

	@Override
	public String getName() {
		return (String) attributes.get("name");
	}
}