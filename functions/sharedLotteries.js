"use strict";

/**
 * Creates share codes for undrawn lotteries and accepts one campsite per code.
 * Only these functions add contributing owners and their campsite IDs.
 */
const { HttpsError } = require("firebase-functions/v2/https");

// Match MAX_LOTTERY_CAMPSITES in the Android model and Firestore rules.
const MAX_LOTTERY_CAMPSITES = 9;

function readId(value, label) {
    if (typeof value !== "string" || !value.trim() || value.includes("/") ||
        value === "." || value === ".." || Buffer.byteLength(value) > 1500) {
        throw new HttpsError("invalid-argument", "Enter a valid " + label + ".");
    }
    return value;
}

function requireActiveOwner(profile) {
    if (!profile.exists || profile.get("isOwner") !== true || profile.get("removed") !== false) {
        throw new HttpsError("permission-denied", "An active campsite owner is required.");
    }
}

/**
 * Returns a single-use code that the creator can give to another owner.
 * The caller supplies the UID from the authenticated callable request.
 */
async function createInvitation(db, uid, data) {
    const lotteryId = readId(data?.lotteryId, "lottery ID");
    const lotteryRef = db.collection("lotteries").doc(lotteryId);
    const invitationRef = db.collection("lotteryOwnerInvitations").doc();
    const profileRef = db.collection("users").doc(uid);

    return db.runTransaction(async (tx) => {
        const [profile, lottery] = await tx.getAll(profileRef, lotteryRef);
        requireActiveOwner(profile);
        if (!lottery.exists || lottery.get("ownerId") !== uid) {
            throw new HttpsError("permission-denied", "Only the lottery creator can invite owners.");
        }
        if (lottery.get("drawCompletedAt") != null) {
            throw new HttpsError("failed-precondition", "This lottery has already been drawn.");
        }
        const campsiteIds = lottery.get("campsiteIds");
        if (!Array.isArray(campsiteIds) || campsiteIds.length === 0) {
            throw new HttpsError("failed-precondition", "The lottery's campsite list is unavailable.");
        }
        if (campsiteIds.length >= MAX_LOTTERY_CAMPSITES) {
            throw new HttpsError("failed-precondition", "This lottery already has nine campsites.");
        }

        tx.create(invitationRef, { lotteryId, creatorId: uid });
        return { code: invitationRef.id };
    });
}

/**
 * Consumes a creator's invitation and adds the accepting owner's active campsite.
 * Retries for the same owner and campsite return the previously accepted result.
 */
async function acceptInvitation(db, uid, data) {
    const code = readId(data?.code, "invitation code");
    const campsiteId = readId(data?.campsiteId, "campsite ID");
    const profileRef = db.collection("users").doc(uid);
    const campsiteRef = db.collection("campsites").doc(campsiteId);
    const invitationRef = db.collection("lotteryOwnerInvitations").doc(code);

    return db.runTransaction(async (tx) => {
        const [profile, campsite, invitation] = await tx.getAll(profileRef, campsiteRef, invitationRef);
        requireActiveOwner(profile);
        if (!campsite.exists || campsite.get("ownerId") !== uid || campsite.get("removed") !== false) {
            throw new HttpsError("permission-denied", "Choose one of your own active campsites.");
        }
        if (!invitation.exists) {
            throw new HttpsError("not-found", "That invitation code could not be found.");
        }

        const lotteryId = readId(invitation.get("lotteryId"), "lottery ID");
        const acceptedBy = invitation.get("acceptedBy");
        if (acceptedBy != null) {
            if (acceptedBy === uid && invitation.get("campsiteId") === campsiteId) {
                return { lotteryId };
            }
            throw new HttpsError("failed-precondition", "That invitation code has already been used.");
        }
        const lotteryRef = db.collection("lotteries").doc(lotteryId);
        const lottery = await tx.get(lotteryRef);
        if (!lottery.exists || lottery.get("ownerId") !== invitation.get("creatorId")) {
            throw new HttpsError("not-found", "This invitation no longer points to a lottery.");
        }
        if (lottery.get("ownerId") === uid) {
            throw new HttpsError("failed-precondition", "Use Edit lottery to add your own campsites.");
        }
        if (lottery.get("drawCompletedAt") != null) {
            throw new HttpsError("failed-precondition", "This lottery has already been drawn.");
        }

        const campsiteIds = lottery.get("campsiteIds");
        const contributed = lottery.get("contributedCampsiteIds") ?? [];
        const owners = lottery.get("contributingOwnerIds") ?? [];
        if (!Array.isArray(campsiteIds) || campsiteIds.length === 0 ||
            !Array.isArray(contributed) || !Array.isArray(owners)) {
            throw new HttpsError("failed-precondition", "The lottery's campsite list is unavailable.");
        }
        if (campsiteIds.includes(campsiteId)) {
            throw new HttpsError("failed-precondition", "That campsite is already in this lottery.");
        }
        if (campsiteIds.length >= MAX_LOTTERY_CAMPSITES) {
            throw new HttpsError("failed-precondition", "This lottery already has nine campsites.");
        }

        const sites = await tx.getAll(
            ...campsiteIds.map((id) => db.collection("campsites").doc(id))
        );
        if (!sites.some((site) => site.exists && site.get("removed") === false &&
            site.get("ownerId") === lottery.get("ownerId"))) {
            throw new HttpsError("failed-precondition", "The creator must have an active campsite in this lottery.");
        }

        tx.update(lotteryRef, {
            campsiteIds: [...campsiteIds, campsiteId],
            contributedCampsiteIds: [...contributed, campsiteId],
            contributingOwnerIds: [...new Set([...owners, uid])]
        });
        tx.update(invitationRef, { acceptedBy: uid, campsiteId });
        return { lotteryId };
    });
}

module.exports = { createInvitation, acceptInvitation };
