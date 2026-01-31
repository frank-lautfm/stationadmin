package de.stationadmin.base.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import de.stationadmin.base.StationAdminClient;
import de.stationadmin.base.playlist.AutoFillRule;
import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.profile.PlaylistProfile;
import de.stationadmin.base.playlist.scheduled.ScheduledItem;
import de.stationadmin.base.playlist.shuffle.TagWeight;
import de.stationadmin.base.playlist.shuffle.TrackRule;
import de.stationadmin.base.playlist.shuffle.TrackRule.FilterType;
import de.stationadmin.base.playlist.validation.PlaylistValidationException;
import de.stationadmin.base.tag.DynamicTag;
import de.stationadmin.base.tag.StaticTag;
import de.stationadmin.base.tag.Tag;
import de.stationadmin.base.tag.TagSet;
import de.stationadmin.gui.playlist.config.shuffle.TagSequenceRule;

public class TagRenameCommand {

	private StationAdminClient client;
	private Tag tag;

	private String status;

	public String getStatus() {
		return status;
	}

	public TagRenameCommand(StationAdminClient client, Tag tag) {
		this.client = client;
		this.tag = tag;
	}

	public void execute(String newName) throws IOException, PlaylistValidationException {
		if (tag.getName().equalsIgnoreCase(newName)) {
			return;
		}
		this.copyTag(newName);
		this.updateDynamicTags(newName);
		this.updateTagSets(newName);
		this.updatePlaylistProfiles(newName);
		this.updateScheduledItems(newName);
		this.updatePlaylists(newName);
		this.deleteOldTag();
	}

	private void copyTag(String newName) throws IOException {
		this.status = "copytag";

		if (tag instanceof StaticTag) {
			StaticTag s = new StaticTag();
			s.setName(newName);
			s.setGroup(s.getGroup());
			client.getTagManager().saveStaticTag(s);

		} else {
			DynamicTag source = (DynamicTag) tag;
			DynamicTag d = new DynamicTag();
			d.setConfiguration(source.getConfiguration());
			d.setName(newName);
			d.setGroup(tag.getGroup());
			client.getTagManager().saveDynamicTag(d);
		}

		int[] trackIds = client.getTagManager().getTrackIds(tag.getName());
		client.getTagManager().tagTracks(newName, trackIds);
	}

	private void updateDynamicTags(String newName) throws IOException {
		this.status = "update.dynamictags";
		for (DynamicTag tag : client.getTagManager().getDynamicTags()) {
			String[] tags = tag.getTags();
			if (updateTagArray(tags, newName)) {
				tag.setTags(tags);
				client.getTagManager().saveDynamicTag(tag);
			}
		}
	}

	private void updateTagSets(String newName) throws IOException, PlaylistValidationException {
		this.status = "update.tagsets";
		for (TagSet tagSet : client.getTagManager().getTagSets()) {
			boolean modified = false;
			if (tagSet.getExcludeTags() != null) {
				String[] excludedTags = tagSet.getExcludeTags();
				if (updateTagArray(excludedTags, newName)) {
					modified = true;
					tagSet.setExcludeTags(excludedTags);
				}
			}
			String[] includedTags = tagSet.getIncludeTags();
			if (updateTagArray(includedTags, newName)) {
				modified = true;
				tagSet.setIncludeTags(includedTags);
			}
			if (modified) {
				client.getTagManager().saveTagSet(tagSet);
			}
		}
	}

	private void updatePlaylistProfiles(String newName) throws IOException, PlaylistValidationException {
		this.status = "update.profiles";
		for (PlaylistProfile profile : this.client.getPlaylistService().getProfiles()) {
			boolean modified = false;
			if (profile.getTrackRules() != null) {
				for (TrackRule rule : profile.getTrackRules().getRules()) {
					if (rule.getFilterType().equals(FilterType.TAG) && rule.getFilter().equals(this.tag.getName())) {
						rule.setFilter(newName);
						modified = true;
					}
				}
			}
			if (profile.getTagWeights() != null) {
				for (TagWeight w : profile.getTagWeights()) {
					if (w.getTag().equals(this.tag.getName())) {
						w.setTag(newName);
						modified = true;
					}
				}
			}
			if (modified) {
				this.client.getPlaylistService().updateProfileOpts(profile.getId());
				this.client.getPlaylistService().saveProfiles();
			}
		}
	}

	private void updateScheduledItems(String newName) throws IOException {
		this.status = "update.scheduleditems";
		for (ScheduledItem item : this.client.getPlaylistService().getScheduledItems()) {
			boolean modified = false;
			if (item.getTag().equals(this.tag.getName())) {
				item.setTag(newName);
				modified = true;
			}
			if (modified) {
				this.client.getPlaylistService().updateScheduledItemsOpts(item.getId(), false);
				this.client.getPlaylistService().saveScheduledItems();
			}
		}
	}

	private void updatePlaylists(String newName) throws IOException, PlaylistValidationException {
		this.status = "update.playlists";

		for (Playlist p : this.client.getPlaylistService().getPlaylistRegistry().getAllPlaylists()) {
			boolean modified = false;

			// shuffle opts
			Map<String, Object> shuffleOpts = p.getShuffleOpts();
			if (shuffleOpts != null) {

				// tag weights
				if (shuffleOpts.containsKey("tagWeights")) {
					Object weightsObj = shuffleOpts.get("tagWeights");
					if (weightsObj instanceof Map) {
						Map<String, Integer> weights = (Map<String, Integer>) weightsObj;
						ArrayList<String> keys = new ArrayList<String>(weights.keySet());
						for (String key : keys) {
							if (key.equals(tag.getName())) {
								Integer value = weights.get(key);
								weights.put(newName, value);
								weights.remove(key);
								modified = true;
							}
						}
					}
				}
				if (shuffleOpts.containsKey("tagPattern")) {
					Object patternObj = shuffleOpts.get("tagPattern");
					if (patternObj instanceof String[]) {
						String[] pattern = (String[]) patternObj;
						if (updateTagArray(pattern, newName)) {
							modified = true;
							shuffleOpts.put("tagPattern", pattern);
						}
					}
					if (patternObj instanceof List) {
						List<String> patternList = (List<String>) patternObj;
						String[] pattern = patternList.toArray(new String[patternList.size()]);
						if (updateTagArray(pattern, newName)) {
							modified = true;
							shuffleOpts.put("tagPattern", pattern);
						}
					}
					if (patternObj instanceof String) {
						String patternStr = (String) patternObj;
						String[] pattern = patternStr.split(",");
						if (updateTagArray(pattern, newName)) {
							modified = true;
							shuffleOpts.put("tagPattern", String.join(",", pattern));
						}
					}
				}
				if (shuffleOpts.containsKey("tagSequences")) {
					List<Map<String, Object>> list = (List<Map<String, Object>>) shuffleOpts.get("tagSequences");
					for (int i = 0; i < list.size(); i++) {
						TagSequenceRule rule = new TagSequenceRule(list.get(i));
						boolean ruleModified = false;
						if (rule.getNext().equals(tag.getName())) {
							rule.setNext(newName);
							ruleModified = true;
						}
						String[] pattern = rule.getPattern();
						if (updateTagArray(pattern, newName)) {
							rule.setPattern(pattern);
							ruleModified = true;
						}
						if (ruleModified) {
							list.set(i, rule.toMap());
							modified = true;
						}
					}

				}
			}

			// autofill rule
			if (p.getAutoFillRule() != null) {
				AutoFillRule rule = p.getAutoFillRule();
				String[] tags = rule.getSourceTags();
				if (updateTagArray(tags, newName)) {
					modified = true;
					rule.setSourceTags(tags);
				}
			}

			if (p.getGeneratePushTag() != null && p.getGeneratePushTag().contains(tag.getName())) {
				String str = updateTagList(p.getGeneratePushTag(), newName, ":");
				if (!str.equals(p.getGeneratePushTag())) {
					modified = true;
					p.setGeneratePushTag(str);
				}
			}

			if (p.getGenerateTags() != null && p.getGenerateTags().contains(tag.getName())) {
				String str = updateTagList(p.getGenerateTags(), newName, null);
				if (!str.equals(p.getGenerateTags())) {
					modified = true;
					p.setGenerateTags(str);
				}
			}

			if (modified) {
				this.client.getPlaylistService().savePlaylist(p);
			}
		}

	}

	private void deleteOldTag() throws IOException {
		this.status = "deletetag";
		client.getTagManager().deleteTag(tag.getName());

	}

	private boolean updateTagArray(String[] tags, String newName) {
		boolean modified = false;
		if (tags != null) {
			for (int i = 0; i < tags.length; i++) {
				if (tags[i].equals(tag.getName())) {
					tags[i] = newName;
					modified = true;
				}
			}
		}
		return modified;
	}

	private String updateTagList(String str, String newName, String inItemSeparator) {
		String[] tags = StringUtils.split(str, ';');
		boolean modified = false;
		for (int i = 0; i < tags.length; i++) {
			if (inItemSeparator != null) {
				if (tags[i].startsWith(tag.getName() + inItemSeparator)) {
					tags[i] = StringUtils.replace(tags[i], tag.getName(), newName);
					modified = true;
				}
			} else {
				if (tags[i].equals(tag.getName())) {
					tags[i] = newName;
					modified = true;
				}
			}
		}
		if (modified) {
			return StringUtils.join(tags, ';');
		}
		return str;

	}

}
