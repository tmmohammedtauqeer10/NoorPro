"use strict";

// 1.1.1: optional composer fields. Unknown or malformed values are dropped (never rejected), so a
// submission from any client version still publishes.
function composerFields(type, data) {
  const out = {};
  if (data.aiLabel === true) out.aiLabel = true;
  if (typeof data.locationName === "string" && data.locationName.trim()) {
    out.locationName = data.locationName.trim().slice(0, 80);
  }
  if (data.visibility === "followers") out.visibility = "followers";
  const poll = data.poll;
  if (["text", "image", "video"].includes(type) && poll && Array.isArray(poll.options)) {
    const options = poll.options.filter((o) => typeof o === "string")
      .map((o) => o.trim().slice(0, 80)).filter((o) => o.length > 0);
    const unique = new Set(options.map((o) => o.toLowerCase()));
    const endsAt = poll.endsAt && typeof poll.endsAt.toMillis === "function" ? poll.endsAt : null;
    const maxEnd = Date.now() + 8 * 24 * 3600 * 1000;
    if (options.length >= 2 && options.length <= 4 && unique.size === options.length &&
        endsAt && endsAt.toMillis() > Date.now() && endsAt.toMillis() <= maxEnd) {
      out.poll = {options, endsAt};
    }
  }
  return out;
}

module.exports = {composerFields};
