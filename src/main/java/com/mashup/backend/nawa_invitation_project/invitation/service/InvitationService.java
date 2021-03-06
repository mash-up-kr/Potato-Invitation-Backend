package com.mashup.backend.nawa_invitation_project.invitation.service;

import com.mashup.backend.nawa_invitation_project.common.aws.AwsS3Service;
import com.mashup.backend.nawa_invitation_project.invitation.domain.Invitation;
import com.mashup.backend.nawa_invitation_project.invitation.domain.InvitationImage;
import com.mashup.backend.nawa_invitation_project.invitation.domain.InvitationImageRepository;
import com.mashup.backend.nawa_invitation_project.invitation.domain.InvitationRepository;
import com.mashup.backend.nawa_invitation_project.invitation.dto.InvitationImageDto;
import com.mashup.backend.nawa_invitation_project.invitation.dto.InvitationWordsRequestDto;
import com.mashup.backend.nawa_invitation_project.invitation.dto.request.InvitationAddressRequestDto;
import com.mashup.backend.nawa_invitation_project.invitation.dto.request.InvitationImagePatchRequestDto;
import com.mashup.backend.nawa_invitation_project.invitation.dto.request.InvitationImageRequestDto;
import com.mashup.backend.nawa_invitation_project.invitation.dto.request.InvitationTimeRequestDto;
import com.mashup.backend.nawa_invitation_project.invitation.dto.response.MapInfoDto;
import com.mashup.backend.nawa_invitation_project.invitation.dto.response.ResDetailInvitationDto;
import com.mashup.backend.nawa_invitation_project.template.domain.Template;
import com.mashup.backend.nawa_invitation_project.template.domain.TemplateRepository;
import com.mashup.backend.nawa_invitation_project.user.domain.User;
import com.mashup.backend.nawa_invitation_project.user.domain.UserRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@Service
public class InvitationService {

  private final UserRepository userRepository;
  private final TemplateRepository templateRepository;
  private final InvitationRepository invitationRepository;
  private final InvitationImageRepository invitationImageRepository;
  private final AwsS3Service awsS3Service;

  @Transactional
  public void updateInvitationWords(String deviceIdentifier,
      InvitationWordsRequestDto invitationWordsRequestDto) {
    User user = userRepository.findByDeviceIdentifier(deviceIdentifier)
        .orElseThrow(() -> new NoSuchElementException()); //TODO : custom exception

    Invitation invitation = invitationRepository.findTopByUsersIdAndTemplatesIdOrderByIdDesc(user.getId(),
        invitationWordsRequestDto.getTemplatesId())
        .orElseThrow(() -> new NoSuchElementException()); //TODO : custom exception

    invitation.updateInvitationWords(
        invitationWordsRequestDto.getInvitationTitle(),
        invitationWordsRequestDto.getInvitationContents()
    );
  }

  @Transactional
  public void updateInvitationTime(String deviceIdentifier,
      InvitationTimeRequestDto invitationTimeRequestDto) {
    User user = userRepository.findByDeviceIdentifier(deviceIdentifier)
        .orElseThrow(() -> new NoSuchElementException()); //TODO : custom exception

    Invitation invitation = invitationRepository.findTopByUsersIdAndTemplatesIdOrderByIdDesc(user.getId(),
        invitationTimeRequestDto.getTemplatesId())
        .orElseThrow(() -> new NoSuchElementException()); //TODO : custom exception

    invitation.updateInvitationTime(invitationTimeRequestDto.getInvitationTime());
  }

  @Transactional
  public void updateInvitationAddress(String deviceIdentifier,
      InvitationAddressRequestDto invitationAddressRequestDto) {
    User user = userRepository.findByDeviceIdentifier(deviceIdentifier)
        .orElseThrow(() -> new NoSuchElementException()); //TODO : custom exception

    Invitation invitation = invitationRepository.findTopByUsersIdAndTemplatesIdOrderByIdDesc(user.getId(),
        invitationAddressRequestDto.getTemplatesId())
        .orElseThrow(() -> new NoSuchElementException()); //TODO : custom exception

    invitation.updateInvitationAddress(
        invitationAddressRequestDto.getInvitationAddressName(),
        invitationAddressRequestDto.getInvitationRoadAddressName(),
        invitationAddressRequestDto.getInvitationPlaceName(),
        invitationAddressRequestDto.getX(),
        invitationAddressRequestDto.getY()
    );
  }

  @Transactional
  public void uploadInvitationImage(String deviceIdentifier,
      InvitationImageRequestDto invitationImageRequestDto, MultipartFile file) throws IOException {
    String imageUrl = awsS3Service.upload(file);
    Optional<User> user = userRepository.findByDeviceIdentifier(deviceIdentifier);
    Optional<Invitation> invitation = invitationRepository
        .findTopByUsersIdAndTemplatesIdOrderByIdDesc(user.get().getId(), invitationImageRequestDto.getTemplateId());
    invitationImageRepository.save(InvitationImage.builder()
        .imageUrl(imageUrl)
        .invitationId(invitation.get().getId())
        .build());
  }

  @Transactional
  public void updateInvitationImage(InvitationImagePatchRequestDto invitationImagePatchRequestDto,
      MultipartFile file) throws IOException {
    String imageUrl = awsS3Service.upload(file);
    Optional<InvitationImage> invitationImage = invitationImageRepository
        .findById(invitationImagePatchRequestDto.getId());
    awsS3Service.delete(invitationImage.get().getImageUrl());
    invitationImage.get().updateImageUrl(imageUrl);
  }

  @Transactional
  public void deleteInvitationImage(Long id)
      throws IOException {
    Optional<InvitationImage> invitationImage = invitationImageRepository
        .findById(id);
    awsS3Service.delete(invitationImage.get().getImageUrl());
    invitationImageRepository.deleteById(id);
  }

  public ResDetailInvitationDto getDetailInvitation(String hashCode) {
    Invitation invitation = invitationRepository.findByHashCode(hashCode)
        .orElseThrow(() -> new IllegalArgumentException("no invitation"));

    Long templatesId = invitation.getTemplatesId();
    Template template = templateRepository.findById(templatesId)
        .orElseThrow(() -> new IllegalArgumentException("no template"));

    MapInfoDto mapInfo = null;
    if (!isCustomPlace(invitation.getX(), invitation.getY())) {
      mapInfo = MapInfoDto.builder()
          .invitationAddressName(invitation.getInvitationAddressName())
          .invitationRoadAddressName(invitation.getInvitationRoadAddressName())
          .x(invitation.getX())
          .y(invitation.getY())
          .build();
    }

    List<InvitationImage> invitationImages = invitationImageRepository
        .findAllByInvitationId(invitation.getId());
    List<InvitationImageDto> invitationImageDtos = new ArrayList<InvitationImageDto>();
    for (InvitationImage invitationImage : invitationImages) {
      InvitationImageDto invitationImageDto = InvitationImageDto.builder()
          .id(invitationImage.getId())
          .imageUrl(invitationImage.getImageUrl())
          .build();
      invitationImageDtos.add(invitationImageDto);
    }

    return ResDetailInvitationDto.builder()
        .templateBackgroundImageUrl(template.getBackgroundImageUrl())
        .templateTypeDescription(template.getTypeDescription())
        .invitationTitle(invitation.getInvitationTitle())
        .invitationContents(invitation.getInvitationContents())
        .invitationTime(invitation.getInvitationTime())
        .invitationPlaceName(invitation.getInvitationPlaceName())
        .mapInfo(mapInfo)
        .invitationImages(invitationImageDtos)
        .build();
  }

  public String getHashCode(String deviceIdentifier, Long templateId) {
    User user = userRepository.findByDeviceIdentifier(deviceIdentifier)
        .orElseThrow(() -> new NoSuchElementException());

    if (!templateRepository.existsById(templateId)) {
      throw new NoSuchElementException();
    }

    Invitation invitation = invitationRepository
        .findTopByUsersIdAndTemplatesIdOrderByIdDesc(user.getId(), templateId)
        .orElseThrow(() -> new NoSuchElementException());

    return invitation.getHashCode();
  }

  private boolean isCustomPlace(Double x, Double y) {
    return !(x != null && y != null);
  }
}
