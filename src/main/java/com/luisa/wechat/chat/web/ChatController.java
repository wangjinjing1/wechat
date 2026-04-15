package com.luisa.wechat.chat.web;

import com.luisa.wechat.chat.service.ChatService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/bootstrap")
    public ChatService.BootstrapView bootstrap(Authentication authentication) {
        return chatService.bootstrap(authentication.getName());
    }

    @GetMapping("/sidebar/conversations")
    public ChatService.PagedResult<ChatService.ConversationSummary> listConversations(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            Authentication authentication) {
        return chatService.listConversations(authentication.getName(), keyword, page, size);
    }

    @GetMapping("/sidebar/users")
    public ChatService.PagedResult<ChatService.UserSummary> listUsers(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            Authentication authentication) {
        return chatService.listUsers(authentication.getName(), keyword, page, size);
    }

    @GetMapping("/sidebar/friend-requests")
    public ChatService.PagedResult<ChatService.FriendRequestView> listFriendRequests(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            Authentication authentication) {
        return chatService.listFriendRequests(authentication.getName(), keyword, page, size);
    }

    @PostMapping("/friends/{username}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addFriend(@PathVariable("username") String username, Authentication authentication) {
        chatService.addFriend(authentication.getName(), username);
    }

    @PostMapping("/friends/{username}/remark")
    public ChatService.UserSummary updateFriendRemark(@PathVariable("username") String username,
                                                      @RequestBody FriendRemarkRequest request,
                                                      Authentication authentication) {
        return chatService.updateFriendRemark(authentication.getName(), username, request.remark());
    }

    @PostMapping("/friends/{username}/remove")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFriend(@PathVariable("username") String username, Authentication authentication) {
        chatService.removeFriend(authentication.getName(), username);
    }

    @PostMapping("/friend-requests/{requestId}/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void acceptFriendRequest(@PathVariable("requestId") Long requestId, Authentication authentication) {
        chatService.acceptFriendRequest(authentication.getName(), requestId);
    }

    @PostMapping("/friend-requests/{requestId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rejectFriendRequest(@PathVariable("requestId") Long requestId, Authentication authentication) {
        chatService.rejectFriendRequest(authentication.getName(), requestId);
    }

    @PostMapping("/groups")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatService.GroupSummary createGroup(@RequestBody GroupRequest request, Authentication authentication) {
        return chatService.createGroup(authentication.getName(), request.name(), request.memberUsernames());
    }

    @PostMapping("/groups/{groupId}/members")
    public ChatService.GroupSummary addGroupMember(@PathVariable("groupId") Long groupId,
                                                   @RequestBody MemberRequest request,
                                                   Authentication authentication) {
        return chatService.addGroupMember(authentication.getName(), groupId, request.username());
    }

    @GetMapping("/groups/{groupId}/members")
    public ChatService.PagedResult<ChatService.GroupMemberView> listGroupMembers(
            @PathVariable("groupId") Long groupId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size,
            Authentication authentication) {
        return chatService.listGroupMembers(authentication.getName(), groupId, keyword, page, size);
    }

    @PostMapping("/groups/{groupId}/members/{memberUserId}/remove")
    public ChatService.GroupSummary removeGroupMember(@PathVariable("groupId") Long groupId,
                                                      @PathVariable("memberUserId") Long memberUserId,
                                                      Authentication authentication) {
        return chatService.removeGroupMember(authentication.getName(), groupId, memberUserId);
    }

    @PostMapping("/me/avatar")
    public ChatService.UserProfile updateAvatar(@RequestParam("file") MultipartFile file, Authentication authentication) {
        return chatService.updateAvatar(authentication.getName(), file);
    }

    @GetMapping("/conversations/lobby/messages")
    public ChatService.MessagePage lobbyMessages(@RequestParam(value = "beforeId", required = false) Long beforeId,
                                                 Authentication authentication) {
        return chatService.listLobbyMessages(authentication.getName(), beforeId);
    }

    @PostMapping("/conversations/lobby/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatService.MessageView sendLobbyMessage(@RequestBody MessageRequest request, Authentication authentication) {
        return chatService.sendLobbyMessage(authentication.getName(), request.type(), request.content());
    }

    @PostMapping("/conversations/lobby/files")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatService.MessageView sendLobbyFile(@RequestParam("file") MultipartFile file, Authentication authentication) {
        return chatService.sendLobbyFile(authentication.getName(), file);
    }

    @GetMapping("/conversations/direct/{username}/messages")
    public ChatService.MessagePage directMessages(@PathVariable("username") String username,
                                                  @RequestParam(value = "beforeId", required = false) Long beforeId,
                                                  Authentication authentication) {
        return chatService.listDirectMessages(authentication.getName(), username, beforeId);
    }

    @PostMapping("/conversations/direct/{username}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatService.MessageView sendDirectMessage(@PathVariable("username") String username,
                                                     @RequestBody MessageRequest request,
                                                     Authentication authentication) {
        return chatService.sendDirectMessage(authentication.getName(), username, request.type(), request.content());
    }

    @PostMapping("/conversations/direct/{username}/files")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatService.MessageView sendDirectFile(@PathVariable("username") String username,
                                                  @RequestParam("file") MultipartFile file,
                                                  Authentication authentication) {
        return chatService.sendDirectFile(authentication.getName(), username, file);
    }

    @GetMapping("/conversations/group/{groupId}/messages")
    public ChatService.MessagePage groupMessages(@PathVariable("groupId") Long groupId,
                                                 @RequestParam(value = "beforeId", required = false) Long beforeId,
                                                 Authentication authentication) {
        return chatService.listGroupMessages(authentication.getName(), groupId, beforeId);
    }

    @PostMapping("/conversations/group/{groupId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatService.MessageView sendGroupMessage(@PathVariable("groupId") Long groupId,
                                                    @RequestBody MessageRequest request,
                                                    Authentication authentication) {
        return chatService.sendGroupMessage(authentication.getName(), groupId, request.type(), request.content());
    }

    @PostMapping("/conversations/group/{groupId}/files")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatService.MessageView sendGroupFile(@PathVariable("groupId") Long groupId,
                                                 @RequestParam("file") MultipartFile file,
                                                 Authentication authentication) {
        return chatService.sendGroupFile(authentication.getName(), groupId, file);
    }

    public record GroupRequest(String name, List<String> memberUsernames) {
    }

    public record MemberRequest(String username) {
    }

    public record MessageRequest(String type, String content) {
    }

    public record FriendRemarkRequest(String remark) {
    }
}
