// 1.1.1: node functions/composer.test.js
"use strict";
const assert = require("assert");
const {composerFields} = require("./composer");
const ts = (ms) => ({toMillis: () => ms});
const soon = ts(Date.now() + 24 * 3600 * 1000);
assert.deepStrictEqual(composerFields("text", {}), {});
assert.deepStrictEqual(composerFields("text", {aiLabel: "yes", visibility: "public", locationName: "  "}), {});
assert.deepStrictEqual(composerFields("image", {aiLabel: true, locationName: " Makkah ", visibility: "followers"}),
  {aiLabel: true, locationName: "Makkah", visibility: "followers"});
assert.strictEqual(composerFields("text", {locationName: "x".repeat(200)}).locationName.length, 80);
assert.deepStrictEqual(composerFields("text", {poll: {options: [" Fajr ", "Isha"], endsAt: soon}}).poll.options, ["Fajr", "Isha"]);
assert.strictEqual(composerFields("reel", {poll: {options: ["a", "b"], endsAt: soon}}).poll, undefined);
assert.strictEqual(composerFields("text", {poll: {options: ["a"], endsAt: soon}}).poll, undefined);
assert.strictEqual(composerFields("text", {poll: {options: ["a", "A"], endsAt: soon}}).poll, undefined);
assert.strictEqual(composerFields("text", {poll: {options: ["a", "b", "c", "d", "e"], endsAt: soon}}).poll, undefined);
assert.strictEqual(composerFields("text", {poll: {options: ["a", "b"], endsAt: ts(Date.now() - 1000)}}).poll, undefined);
assert.strictEqual(composerFields("text", {poll: {options: ["a", "b"], endsAt: ts(Date.now() + 30 * 86400000)}}).poll, undefined);
console.log("composer.test.js: all passed");
