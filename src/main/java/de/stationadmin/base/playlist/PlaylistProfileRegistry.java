package de.stationadmin.base.playlist;

import java.util.List;

import de.stationadmin.base.playlist.profile.PlaylistProfile;

public interface PlaylistProfileRegistry {

  List<PlaylistProfile> getProfiles();

  PlaylistProfile getProfile(String id);

}
