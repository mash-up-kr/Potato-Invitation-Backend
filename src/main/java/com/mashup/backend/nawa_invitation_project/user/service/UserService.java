package com.mashup.backend.nawa_invitation_project.user.service;

import com.mashup.backend.nawa_invitation_project.common.customUtil.CustomUtil;
import com.mashup.backend.nawa_invitation_project.invitation.domain.Invitation;
import com.mashup.backend.nawa_invitation_project.invitation.domain.InvitationRepository;
import com.mashup.backend.nawa_invitation_project.template.domain.Template;
import com.mashup.backend.nawa_invitation_project.template.domain.TemplateRepository;
import com.mashup.backend.nawa_invitation_project.user.domain.User;
import com.mashup.backend.nawa_invitation_project.user.domain.UserRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserService {

  private final UserRepository userRepository;
  private final TemplateRepository templateRepository;
  private final InvitationRepository invitationRepository;


  @Transactional
  public void findUserOrCreateUserAndInvitations(String deviceIdentifier) {
    Optional<User> user = userRepository.findByDeviceIdentifier(deviceIdentifier);
    if (user.isPresent()) {
      return;
    }
    User newUser = userRepository.save(User.builder().deviceIdentifier(deviceIdentifier).build());
    List<Template> templates = templateRepository.findAll();
    templates.forEach(template -> {
          Invitation newInvitation = invitationRepository.save(
              Invitation.builder()
              .usersId(newUser.getId())
              .templatesId(template.getId())
              .build());
          String hashCodeUsingId = CustomUtil.getHashCode(newInvitation.getId());
          newInvitation.updateHashCode(hashCodeUsingId);
        }
    );
  }
}
