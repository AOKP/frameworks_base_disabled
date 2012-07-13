/*
 * Copyright (c) 2010, Texas Instruments Incorporated
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * *  Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * *  Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * *  Neither the name of Texas Instruments Incorporated nor the names of
 *    its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/* -------------------------------------------------------------------------- */
/*
 * @file:Omx_ti_video.h
 * This header defines the structures specific to the param or config indices of Openmax Video Component.
 *
 * @path:
 * \WTSD_DucatiMMSW\ omx\omx_il_1_x\omx_core\

 * -------------------------------------------------------------------------- */

/* =========================================================================
 *!
 *! Revision History
 *! =====================================================================
 *! 24-Dec-2008  Navneet 	navneet@ti.com  	Initial Version
 *! 14-Jul-2009	Radha Purnima  radhapurnima@ti.com
 *! 25-Aug-2009	Radha Purnima  radhapurnima@ti.com
 * =========================================================================*/


#ifndef OMX_TI_VIDEO_H
#define OMX_TI_VIDEO_H
#define H264ENC_MAXNUMSLCGPS 2
#define OMXH264E_MAX_SLICE_SUPPORTED 64
#include <OMX_Core.h>

/**
 *	@brief	mode selection for the data that is given to the Codec
 */

typedef enum OMX_VIDEO_DATASYNCMODETYPE {
    OMX_Video_FixedLength,	//!<  Interms of multiples of 4K
    OMX_Video_SliceMode,		//!<  Slice mode
    OMX_Video_NumMBRows,	//!< Number of rows, each row is 16 lines of video
    OMX_Video_EntireFrame  	//!< Processing of entire frame data
} OMX_VIDEO_DATASYNCMODETYPE;


/* ========================================================================== */
/*!
 @brief OMX_TI_VIDEO_PARAM_DATAMODE  :to configure how the input and output data is fed to the Codec
 @param  nPortIndex  to specify the index of the port
 @param  eDataMode 	to specify the data mode
 						@sa  OMX_VIDEO_DATASYNCMODETYPE
 @param  nNumDataUnits	 to specify the number of data units (where units are of type defined by eDataMode)
 */
/* ==========================================================================*/
typedef struct OMX_VIDEO_PARAM_DATASYNCMODETYPE{
	OMX_U32 nSize;
	OMX_VERSIONTYPE nVersion;
	OMX_U32 nPortIndex;
	OMX_VIDEO_DATASYNCMODETYPE eDataMode;
	OMX_U32 nNumDataUnits;
} OMX_VIDEO_PARAM_DATASYNCMODETYPE;

/**
 *	@brief	Aspect Ratio type selection for the encoded bit stream
 */
typedef enum OMX_VIDEO_ASPECTRATIOTYPE{
	OMX_Video_AR_Unspecified,  //!< Unspecified aspect ratio
	OMX_Video_AR_Square ,  //!< 1:1 (square) aspect ratio
	OMX_Video_AR_12_11  ,  //!<  12:11  aspect ratio
	OMX_Video_AR_10_11  ,  //!<  10:11  aspect ratio
	OMX_Video_AR_16_11  ,  //!<  16:11  aspect ratio
	OMX_Video_AR_40_33  ,  //!<  40:33  aspect ratio
	OMX_Video_AR_24_11  ,  //!<  24:11  aspect ratio
	OMX_Video_AR_20_11  ,  //!<  20:11  aspect ratio
	OMX_Video_AR_32_11  ,  //!<  32:11  aspect ratio
	OMX_Video_AR_80_33  ,  //!<  80:33  aspect ratio
	OMX_Video_AR_18_11  ,  //!<  18:11  aspect ratio
	OMX_Video_AR_15_15  ,  //!<  15:15  aspect ratio
	OMX_Video_AR_64_33  ,  //!<  64:33  aspect ratio
	OMX_Video_AR_160_99 ,  //!<  160:99 aspect ratio
	OMX_Video_AR_4_3    ,  //!<  4:3    aspect ratio
	OMX_Video_AR_3_2    ,  //!<  3:2    aspect ratio
	OMX_Video_AR_2_1    ,  //!<  2:1    aspect ratio
	OMX_Video_AR_Extended = 255,       //!<  Extended aspect ratio
   	OMX_Video_AR_Extended_MAX =  0X7FFFFFFF
}OMX_VIDEO_ASPECTRATIOTYPE;


/* ========================================================================== */
/*!
 @brief OMX_TI_VIDEO_PARAM_VUI_SELECT  :to select the VUI Settings
 @param  bAspectRatioPresent flag to indicate the insertion of aspect ratio information in VUI part of bit-stream
 @param  ePixelAspectRatio to specify the Aspect Ratio
 @param  bFullRange to indicate whether pixel value range is specified as full range or not (0 to 255)
*/
/* ==========================================================================*/
typedef struct OMX_VIDEO_PARAM_VUIINFOTYPE {
	OMX_U32	 nSize;
	OMX_VERSIONTYPE nVersion;
	OMX_U32	 nPortIndex;
	OMX_BOOL bAspectRatioPresent;
	OMX_VIDEO_ASPECTRATIOTYPE ePixelAspectRatio;
	OMX_BOOL bFullRange;
}OMX_VIDEO_PARAM_VUIINFOTYPE;

/* ========================================================================== */
/*!
 @brief OMX_VIDEO_CONFIG_PIXELINFOTYPE  :to specify the information related to the input pixel data (aspect ratio & range) to the Codec
 										so that codec can incorporate this info in the coded bit stream
 @param  nWidth 	 to specify the Aspect ratio: width of the pixel
 @param  nHeight 	 to specify the Aspect ratio: height of the pixel
 */
/* ==========================================================================*/
typedef struct OMX_VIDEO_CONFIG_PIXELINFOTYPE  {
	OMX_U32 nSize;
	OMX_VERSIONTYPE nVersion;
	OMX_U32 nPortIndex;
	OMX_U32 nWidth;
	OMX_U32 nHeight;
} OMX_VIDEO_CONFIG_PIXELINFOTYPE;

/* ========================================================================== */
/*!
 @brief OMX_VIDEO_PARAM_AVCNALUCONTROLTYPE : to configure what NALU  need to send along the frames of different types (Intra,IDR...etc)
 @param  nStartofSequence 	to configure the different NALU (specified via enabling/disabling (1/0) the bit positions) that need to send along with the Start of sequence frame
 @param  nEndofSequence	 	to to configure the different NALU (specified via enabling/disabling (1/0) the bit positions) that need to send along with the End of sequence frame
 @param  nIDR 				to to configure the different NALU (specified via enabling/disabling (1/0) the bit positions) that need to send along with the IDR frame
 @param  nIntraPicture	  		to to configure the different NALU (specified via enabling/disabling (1/0) the bit positions) that need to send along with the Intra frame
 @param  nNonIntraPicture	  	to to configure the different NALU (specified via enabling/disabling (1/0) the bit positions) that need to send along with the Non Intra frame

Bit Position:   13|       12|      11|           10|      9|    8|    7|   6|      5|         4|              3|              2|              1|          0
NALU TYPE:  SPS_VUI|FILLER|EOSTREAM|EOSEQ|AUD|PPS|SPS|SEI|IDR_SLICE|SLICE_DP_C|SLICE_DP_B|SLICE_DP_A|SLICE|UNSPECIFIED \n
*/
/* ==========================================================================*/
typedef struct OMX_VIDEO_PARAM_AVCNALUCONTROLTYPE {
	OMX_U32	 nSize;
	OMX_VERSIONTYPE nVersion;
	OMX_U32	 	nPortIndex;
	OMX_U32 	nStartofSequence;
	OMX_U32 	nEndofSequence;
	OMX_U32 	nIDR;
	OMX_U32 	nIntraPicture;
	OMX_U32 	nNonIntraPicture;
}OMX_VIDEO_PARAM_AVCNALUCONTROLTYPE;


/* ========================================================================== */
/*!
 @brief OMX_VIDEO_CONFIG_MESEARCHRANGETYPE : to configure Motion Estimation Parameters
 @param  eMVAccuracy 		to specify the Motion Vector Accuracy
 							@sa OMX_VIDEO_MOTIONVECTORTYPE
 @param  sHorSearchRangeP	 	to Specify the Horizontal Search range for P Frame
 @param  sVerSearchRangeP		to Specify the Vertical Search range for P Frame
 @param  sHorSearchRangeB	  	to Specify the Horizontal Search range for B Frame
 @param  sVerSearchRangeB	  	to Specify the Vertical Search range for B Frame
*/
/* ==========================================================================*/
typedef struct OMX_VIDEO_CONFIG_MESEARCHRANGETYPE{
	OMX_U32	 nSize;
	OMX_VERSIONTYPE nVersion;
	OMX_U32	 nPortIndex;
	OMX_VIDEO_MOTIONVECTORTYPE eMVAccuracy;
	OMX_U32	 nHorSearchRangeP;
	OMX_U32	 nVerSearchRangeP;
	OMX_U32	 nHorSearchRangeB;
	OMX_U32	 nVerSearchRangeB;
}OMX_VIDEO_CONFIG_MESEARCHRANGETYPE;

/**
 *	@brief	Block size specification
 */
typedef enum OMX_VIDEO_BLOCKSIZETYPE {
	OMX_Video_Block_Size_16x16=0,
	OMX_Video_Block_Size_8x8,
	OMX_Video_Block_Size_8x4,
	OMX_Video_Block_Size_4x8,
	OMX_Video_Block_Size_4x4,
   	OMX_Video_Block_Size_MAX =  0X7FFFFFFF
}OMX_VIDEO_BLOCKSIZETYPE;

/* ========================================================================== */
/*!
 @brief OMX_VIDEO_PARAM_MEBLOCKSIZETYPE : to configure the Min Motion Estimation block size for P and B frames
 @param  eMinBlockSizeP 		to specify the Min Block size used for Motion Estimation incase of P Frames
 							@sa OMX_VIDEO_BLOCKSIZETYPE
 @param  eMinBlockSizeB	 	to specify the Min Block size used for Motion Estimation incase of B Frames
*/
/* ==========================================================================*/
typedef struct OMX_VIDEO_PARAM_MEBLOCKSIZETYPE{
	OMX_U32	 nSize;
	OMX_VERSIONTYPE nVersion;
	OMX_U32	 nPortIndex;
	OMX_VIDEO_BLOCKSIZETYPE eMinBlockSizeP;
	OMX_VIDEO_BLOCKSIZETYPE eMinBlockSizeB;
}OMX_VIDEO_PARAM_MEBLOCKSIZETYPE;

/**
 *	@brief	to select the chroma component used for Intra Prediction
 */
typedef enum OMX_VIDEO_CHROMACOMPONENTTYPE {
	OMX_Video_Chroma_Component_Cr_Only=0,	//!< consider only Cr chroma component for Intra prediction
	OMX_Video_Chroma_Component_Cb_Cr_Both,  //!< consider both (Cb & Cr) chroma components for Intra prediction
     OMX_Video_Chroma_Component_MAX =  0X7FFFFFFF
}OMX_VIDEO_CHROMACOMPONENTTYPE;

/* ========================================================================== */
/*!
 @brief OMX_VIDEO_PARAM_INTRAPREDTYPE : to configure the Modes for the different block sizes during Intra Prediction
 @param  nLumaIntra4x4Enable 	 	to configure the Modes for 4x4 block size during luma intra prediction: bit position specifies the modes that are enabled/disabled
								 HOR_UP|VERT_LEFT|HOR_DOWN|VERT_RIGHT|DIAG_DOWN_RIGHT|DIAG_DOWN_LEFT|DC|HOR|VER
 @param  nLumaIntra8x8Enable	 	to configure the Modes for 4x4 block size during luma intra prediction
 								HOR_UP|VERT_LEFT|HOR_DOWN|VERT_RIGHT|DIAG_DOWN_RIGHT|DIAG_DOWN_LEFT|DC|HOR|VER
 @param  nLumaIntra16x16Enable	 	to configure the Modes for 4x4 block size during luma intra prediction
								 PLANE|DC|HOR|VER
 @param  nChromaIntra8x8Enable	 	to configure the Modes for 4x4 block size during luma intra prediction
								 PLANE|DC|HOR|VER
 @param  eChromaComponentEnable	to select the chroma components used for the intra prediction
 								@sa OMX_VIDEO_CHROMACOMPONENTTYPE
*/
/* ==========================================================================*/
typedef struct OMX_VIDEO_PARAM_INTRAPREDTYPE{
	OMX_U32	 nSize;
	OMX_VERSIONTYPE nVersion;
	OMX_U32	 nPortIndex;
	OMX_U32	 nLumaIntra4x4Enable;
	OMX_U32 nLumaIntra8x8Enable;
	OMX_U32 nLumaIntra16x16Enable;
	OMX_U32 nChromaIntra8x8Enable;
	OMX_VIDEO_CHROMACOMPONENTTYPE eChromaComponentEnable;
}OMX_VIDEO_PARAM_INTRAPREDTYPE;


/**
 *	@brief	Encoding Mode Preset
 */
typedef enum OMX_VIDEO_ENCODING_MODE_PRESETTYPE {
	OMX_Video_Enc_Default=0, 	//!<  for all the params default values are taken
	OMX_Video_Enc_High_Quality, //!<  todo: mention the parameters that takes specific values depending on this selection
	OMX_Video_Enc_User_Defined,
	OMX_Video_Enc_High_Speed_Med_Quality,
	OMX_Video_Enc_Med_Speed_Med_Quality,
	OMX_Video_Enc_Med_Speed_High_Quality,
	OMX_Video_Enc_High_Speed,
   	OMX_Video_Enc_Preset_MAX =  0X7FFFFFFF
}OMX_VIDEO_ENCODING_MODE_PRESETTYPE;

/**
 *	@brief	Rate Control Preset
 */
typedef enum OMX_VIDEO_RATECONTROL_PRESETTYPE {
	OMX_Video_RC_Low_Delay,	//!<todo:  mention the parameters that takes specific values depending on this selection
	OMX_Video_RC_Storage,
	OMX_Video_RC_Twopass,
	OMX_Video_RC_None,
	OMX_Video_RC_User_Defined,
   	OMX_Video_RC_MAX =  0X7FFFFFFF
}OMX_VIDEO_RATECONTROL_PRESETTYPE;

/* ========================================================================== */
/*!
 @brief OMX_VIDEO_PARAM_ENCODER_PRESETTYPE : to select the preset for Encoding Mode & Rate Control
 @param  eEncodingModePreset		to specify Encoding Mode Preset
 							@sa OMX_VIDEO_ENCODING_MODE_PRESETTYPE
 @param  eRateControlPreset	to specify Rate Control Preset
 							@sa OMX_VIDEO_RATECONTROL_PRESETTYPE
*/
/* ==========================================================================*/
typedef struct OMX_VIDEO_PARAM_ENCODER_PRESETTYPE{
	OMX_U32	 nSize;
	OMX_VERSIONTYPE nVersion;
	OMX_U32	 nPortIndex;
	OMX_VIDEO_ENCODING_MODE_PRESETTYPE eEncodingModePreset;
	OMX_VIDEO_RATECONTROL_PRESETTYPE eRateControlPreset;
}OMX_VIDEO_PARAM_ENCODER_PRESETTYPE;


/**
 *	@brief	 input content type
 */
typedef enum OMX_TI_VIDEO_FRAMECONTENTTYPE {
	OMX_TI_Video_Progressive = 0,			//!<Progressive frame
	OMX_TI_Video_Interlace_BothFieldsTogether = 1,	//!<Interlaced frame
	OMX_TI_Video_Interlace_OneField = 2,
	OMX_TI_Video_AVC_2004_StereoInfoType = 3,
	OMX_TI_Video_AVC_2010_StereoFramePackingType = 4,
	OMX_TI_Video_FrameContentType_MAX = 0x7FFFFFFF
}OMX_TI_VIDEO_FRAMECONTENTTYPE;

/**
 *	@brief	 Specifies the type of interlace content
 */
typedef enum OMX_TI_VIDEO_AVC_INTERLACE_CODINGTYPE {
	OMX_TI_Video_Interlace_PICAFF = 0,	//!< PicAFF type of interlace coding
	OMX_TI_Video_Interlace_MBAFF,		//!< MBAFF type of interlace coding
	OMX_TI_Video_Interlace_Fieldonly,	//!< Field only coding
	OMX_TI_Video_Interlace_Fieldonly_MRF=OMX_TI_Video_Interlace_Fieldonly,
	OMX_TI_Video_Interlace_Fieldonly_ARF,
	OMX_TI_Video_Interlace_Fieldonly_SPF,	//!< Field only coding where codec decides the partiy of the field to be used based upon content
	OMX_Video_Interlace_MAX = 0x7FFFFFFF
}OMX_TI_VIDEO_AVC_INTERLACE_CODINGTYPE;

/* ========================================================================== */
/*!
 @brief OMX_TI_VIDEO_PARAM_FRAMEDATACONTENTTYPE : to configure the data content
 @param  eContentType		to specify Content type
 							@sa OMX_VIDEO_FRAMECONTENTTYPE
*/
/* ==========================================================================*/
typedef struct OMX_TI_VIDEO_PARAM_FRAMEDATACONTENTTYPE{
	OMX_U32	 nSize;
	OMX_VERSIONTYPE nVersion;
	OMX_U32	 nPortIndex;
	OMX_TI_VIDEO_FRAMECONTENTTYPE eContentType;
}OMX_TI_VIDEO_PARAM_FRAMEDATACONTENTTYPE;

/* ========================================================================== */
/*!
 @brief OMX_TI_VIDEO_PARAM_AVCINTERLACECODING : to configure the interlace encoding related settings
 @param  eInterlaceCodingType	to specify the settings of interlace content
 							@sa OMX_VIDEO_INTERLACE_CODINGTYPE
 @param  bTopFieldFirst				to specify the first field sent is top or bottom
 @param  bBottomFieldIntra		to specify codec that encode bottomfield also as intra or not
*/
/* ==========================================================================*/
typedef struct OMX_TI_VIDEO_PARAM_AVCINTERLACECODING{
	OMX_U32	 nSize;
	OMX_VERSIONTYPE nVersion;
	OMX_U32	 nPortIndex;
	OMX_TI_VIDEO_AVC_INTERLACE_CODINGTYPE eInterlaceCodingType;
	OMX_BOOL bTopFieldFirst;
	OMX_BOOL bBottomFieldIntra;
}OMX_TI_VIDEO_PARAM_AVCINTERLACECODING;
/* ========================================================================== */
/*!
 @brief OMX_TI_VIDEO_PARAM_AVCENC_STEREOINFO2004  : to configure the 2004 related stereo information type
*/
/* ==========================================================================*/

typedef struct OMX_TI_VIDEO_PARAM_AVCENC_STEREOINFO2004
{
	OMX_U32          nSize;
	OMX_VERSIONTYPE  nVersion;
	OMX_U32          nPortIndex;
	OMX_BOOL         btopFieldIsLeftViewFlag;
	OMX_BOOL         bViewSelfContainedFlag;
} OMX_TI_VIDEO_AVCENC_STEREOINFO2004;

typedef enum OMX_TI_VIDEO_AVCSTEREO_FRAMEPACKTYPE{
	OMX_TI_Video_FRAMEPACK_CHECKERBOARD        = 0,
	OMX_TI_Video_FRAMEPACK_COLUMN_INTERLEAVING = 1,
	OMX_TI_Video_FRAMEPACK_ROW_INTERLEAVING    = 2,
	OMX_TI_Video_FRAMEPACK_SIDE_BY_SIDE        = 3,
	OMX_TI_Video_FRAMEPACK_TOP_BOTTOM          = 4,
	OMX_TI_Video_FRAMEPACK_TYPE_DEFAULT        = OMX_TI_Video_FRAMEPACK_SIDE_BY_SIDE,
	OMX_TI_Video_FRAMEPACK_TYPE_MAX = 0x7FFFFFFF
} OMX_TI_VIDEO_AVCSTEREO_FRAMEPACKTYPE;

/* ========================================================================== */
/*!
 @brief OMX_TI_VIDEO_PARAM_AVCENC_FRAMEPACKINGINFO2010 : to configure the 2010 related stereo information type
*/
/* ==========================================================================*/

typedef struct OMX_TI_VIDEO_PARAM_AVCENC_FRAMEPACKINGINFO2010
{
	OMX_U32          nSize;
	OMX_VERSIONTYPE nVersion;
	OMX_U32          nPortIndex;
	OMX_TI_VIDEO_AVCSTEREO_FRAMEPACKTYPE eFramePackingType;
	OMX_U8         nFrame0PositionX;
	OMX_U8         nFrame0PositionY;
	OMX_U8         nFrame1PositionX;
	OMX_U8         nFrame1PositionY;
}OMX_TI_VIDEO_PARAM_AVCENC_FRAMEPACKINGINFO2010;

/**
 *	@brief	 Specifies Transform Block Size
 */
typedef enum OMX_VIDEO_TRANSFORMBLOCKSIZETYPE {
	OMX_Video_Transform_Block_Size_4x4 =0,	//!< Transform blocks size is 8x8 : Valid for only High Profile
	OMX_Video_Transform_Block_Size_8x8,	//!< Transform blocks size is 4x4
	OMX_Video_Transform_Block_Size_Adaptive, //!< Adaptive transform block size : encoder decides as per content
    OMX_Video_Transform_Block_Size_MAX =  0X7FFFFFFF
}OMX_VIDEO_TRANSFORMBLOCKSIZETYPE;

/* ========================================================================== */
/*!
 @brief OMX_VIDEO_PARAM_TRANSFORM_BLOCKSIZETYPE : to select the Block Size used for transformation
 @param  eTransformBlocksize	to specify Block size used for transformation
 							@sa OMX_VIDEO_TRANSFORMBLOCKSIZETYPE
*/
/* ==========================================================================*/

typedef struct OMX_VIDEO_PARAM_TRANSFORM_BLOCKSIZETYPE{
	OMX_U32	 nSize;
	OMX_VERSIONTYPE nVersion;
	OMX_U32	 nPortIndex;
	OMX_VIDEO_TRANSFORMBLOCKSIZETYPE eTransformBlocksize;
}OMX_VIDEO_PARAM_TRANSFORM_BLOCKSIZETYPE;


/* ========================================================================== */
/*!
 @brief OMX_VIDEO_CONFIG_SLICECODINGTYPE : to configure the Slice Settings
 @param  eSliceMode	to specify the Slice mode
 							@sa OMX_VIDEO_AVCSLICEMODETYPE
 @param  nSlicesize to specify the sliceSize
*/
/* ==========================================================================*/

typedef struct OMX_VIDEO_CONFIG_SLICECODINGTYPE{
	OMX_U32	 nSize;
	OMX_VERSIONTYPE nVersion;
	OMX_U32	 nPortIndex;
	OMX_VIDEO_AVCSLICEMODETYPE eSliceMode;
	OMX_U32	 nSlicesize;
}OMX_VIDEO_CONFIG_SLICECODINGTYPE;

/**
 *	@brief	 Specifies Slice Group Change Direction Flag
 */
typedef enum OMX_VIDEO_SLIGRPCHANGEDIRTYPE{
  OMX_Video_Raster_Scan             = 0 , //!< 0 : Raster scan order
  OMX_Video_Clockwise              = 0 , //!< 0 : Clockwise (used for BOX OUT FMO Params)
  OMX_Video_Right                   = 0 , //!< 0 : RIGHT (Used for Wipe FMO type)
  OMX_Video_Reverse_Raster_Scan= 1 , //!< 1 : Reverse Raster Scan Order
  OMX_Video_Counter_Clockwise       = 1 , //!< 1 : Counter Clockwise (used for BOX OUT FMO Params)
  OMX_Video_Left                    = 1,  //!< 1 : LEFT (Used for Wipe FMO type)
  OMX_Video_Left_MAX =  0X7FFFFFFF
} OMX_VIDEO_SLICEGRPCHANGEDIRTYPE;

/* ========================================================================== */
/*!
 @brief OMX_TI_VIDEO_PARAM_FMO_ADVANCEDSETTINGS : to configure the FMO Settings
 @param
*/
/* ==========================================================================*/
typedef struct OMX_VIDEO_PARAM_AVCADVANCEDFMOTYPE{
	OMX_U32	 nSize;
	OMX_VERSIONTYPE nVersion;
	OMX_U32 nPortIndex;
	OMX_U8 nNumSliceGroups;
	OMX_U8 nSliceGroupMapType;
	OMX_VIDEO_SLICEGRPCHANGEDIRTYPE eSliceGrpChangeDir;
	OMX_U32 nSliceGroupChangeRate;
	OMX_U32 nSliceGroupChangeCycle;
	OMX_U32 nSliceGroupParams[H264ENC_MAXNUMSLCGPS] ;
}OMX_VIDEO_PARAM_AVCADVANCEDFMOTYPE;

/* ========================================================================== */
/*!
 @brief OMX_TI_VIDEO_CONFIG_QPSETTINGS : to configure the Qp Settings of I, P &B Frames
 @param  nQpI
*/
/* ==========================================================================*/

typedef struct OMX_VIDEO_CONFIG_QPSETTINGSTYPE{
	OMX_U32	 nSize;
	OMX_VERSIONTYPE nVersion;
	OMX_U32	 nPortIndex;
	OMX_U32	 nQpI;
	OMX_U32	 nQpMaxI;
	OMX_U32	 nQpMinI;
	OMX_U32	 nQpP;
	OMX_U32	 nQpMaxP;
	OMX_U32	 nQpMinP;
	OMX_U32	 nQpOffsetB;
	OMX_U32	 nQpMaxB;
	OMX_U32	 nQpMinB;
}OMX_VIDEO_CONFIG_QPSETTINGSTYPE;

/* ========================================================================== */
/*!
 @brief OMX_TI_VIDEO_PARAM_AVCHRDBUFFERSETTING : to configure the HRD
	(Hypothetical Reference Decoder) related params
 @param  nInitialBufferLevel	Initial buffer level for HRD compliance
 @param  nHRDBufferSize		Hypothetical Reference Decoder buffer size
 @param  nTargetBitrate		Target bitrate to encode with
*/
/* ==========================================================================*/

typedef struct OMX_TI_VIDEO_PARAM_AVCHRDBUFFERSETTING {
	OMX_U32     nSize;
	OMX_VERSIONTYPE     nVersion;
	OMX_U32    nPortIndex;
	OMX_U32    nInitialBufferLevel;
	OMX_U32    nHRDBufferSize;
	OMX_U32    nTargetBitrate;
} OMX_TI_VIDEO_PARAM_AVCHRDBUFFERSETTING;

/* ========================================================================== */
/*!
 @brief OMX_TI_VIDEO_CONFIG_AVCHRDBUFFERSETTING : to configure the HRD
	(Hypothetical Reference Decoder) related params
 @param  nHRDBufferSize		Hypothetical Reference Decoder Buffer Size
 @param  nEncodeBitrate		Target bitrate to encode with

*/
/* ==========================================================================*/

typedef struct OMX_TI_VIDEO_CONFIG_AVCHRDBUFFERSETTING {
	OMX_U32    nSize;
	OMX_VERSIONTYPE     nVersion;
	OMX_U32     nPortIndex;
	OMX_U32     nHRDBufferSize;
	OMX_U32     nEncodeBitrate;
} OMX_TI_VIDEO_CONFIG_AVCHRDBUFFERSETTING;

/* ========================================================================= */
/*!
 @brief OMX_TI_VIDEO_CODINGTYPE :
	Extension to video coding type enum for VP6 and VP7
 @param
*/
/* ==========================================================================*/

typedef enum OMX_TI_VIDEO_CODINGTYPE {
	OMX_VIDEO_CodingVP6 =
		(OMX_VIDEO_CODINGTYPE) OMX_VIDEO_CodingVendorStartUnused +1,  /* VP6 */
	OMX_VIDEO_CodingVP7, /* VP7 */
        OMX_TI_VIDEO_CodingSORENSONSPK, /* Sorenson spark*/
        OMX_VIDEO_CodingSVC,    /**< H.264/SVC */
	OMX_VIDEO_CodingVP8 /* VP8 */
}OMX_TI_VIDEO_CODINGTYPE;


/* ========================================================================= */
/*!
 @brief OMX_TI_VIDEO_MPEG4LEVELTYPE:
        Extension to MPEG-4 level to cater to level 6
 @param
*/
/* ==========================================================================*/
typedef enum OMX_TI_VIDEO_MPEG4LEVELTYPE {
        OMX_TI_VIDEO_MPEG4Level6  =
            (OMX_VIDEO_MPEG4LEVELTYPE) OMX_VIDEO_MPEG4LevelVendorStartUnused + 1
} OMX_TI_VIDEO_MPEG4LEVELTYPE;



/**
 *	@brief	 Specifies various intra refresh methods
 */
typedef enum OMX_TI_VIDEO_INTRAREFRESHTYPE {
    OMX_TI_VIDEO_IntraRefreshNone = 0,
    OMX_TI_VIDEO_IntraRefreshCyclicMbs,
    OMX_TI_VIDEO_IntraRefreshCyclicRows,
    OMX_TI_VIDEO_IntraRefreshMandatory,
    OMX_TI_VIDEO_IntraRefreshMax = 0x7FFFFFFF
} OMX_TI_VIDEO_INTRAREFRESHTYPE;


/* ========================================================================== */
/*!
 @brief OMX_TI_VIDEO_PARAM_INTRAREFRESHTYPE  : Configuration parameters for
                                               intra refresh settings
 @param  eRefreshMode		Various refresh modes supported
 @param  nIntraRefreshRate 	Intra refresh rate
*/
/* ==========================================================================*/

typedef struct OMX_TI_VIDEO_PARAM_INTRAREFRESHTYPE {
    OMX_U32 nSize;
    OMX_VERSIONTYPE nVersion;
    OMX_U32 nPortIndex;
    OMX_TI_VIDEO_INTRAREFRESHTYPE eRefreshMode;
    OMX_U32 nIntraRefreshRate;
} OMX_TI_VIDEO_PARAM_INTRAREFRESHTYPE;


/* ============================================================================= */
/*!
 @brief OMX_TI_STEREODECINFO : Structure to access 2004 SEI message generated by
 H264 decoder as metatadata on its output port.
 */
/* ============================================================================= */

typedef struct OMX_TI_STEREODECINFO {
	OMX_U32 nFieldViewsFlag;
	OMX_U32 nTopFieldIsLeftViewFlag;
	OMX_U32 nCurrentFrameIsLeftViewFlag;
	OMX_U32 nNextFrameIsSecondViewFlag;
	OMX_U32 nLeftViewSelfContainedFlag;
	OMX_U32 nRightViewSelfContainedFlag;
} OMX_TI_STEREODECINFO;

typedef struct OMX_TI_FRAMEPACKINGDECINFO {
	OMX_U32 nFramePackingArrangementId;
	OMX_U32 nFramePackingArrangementRepetitionPeriod;
	OMX_U8  nFramePackingArrangementCancelFlag;
	OMX_U8  nFramePackingArrangementType;
	OMX_U8  nQuincunxSamplingFlag;
	OMX_U8  nContentInterpretationType;
	OMX_U8  nSpatialFlippingFlag;
	OMX_U8  nFrame0FlippedFlag;
	OMX_U8  nFieldViewsFlag;
	OMX_U8  nCurrentFrameIsFrame0Flag;
	OMX_U8  nFrame0SelfContainedFlag;
	OMX_U8  nFrame1SelfContainedFlag;
	OMX_U8  nFrame0GridPositionX;
	OMX_U8  nFrame0GridPositionY;
	OMX_U8  nFrame1GridPositionX;
	OMX_U8  nFrame1GridPositionY;
	OMX_U8  nFramePackingArrangementReservedByte;
	OMX_U8  nFramePackingArrangementExtensionFlag;
} OMX_TI_FRAMEPACKINGDECINFO;

/* ============================================================================= */
/*!
 @brief OMX_TI_VIDEO_RANGEMAPPING : Structure to access luma and chroma range
                                    mapping generated by decoders as
                                    metatadata on its output port.
 @param nRangeMappingLuma     Luma scale factor for range mapping.
 @param nRangeMappingChroma   Chroma scale factor for range mapping.
*/
/* ============================================================================= */

typedef struct OMX_TI_VIDEO_RANGEMAPPING {
	OMX_U32 nRangeMappingLuma;
	OMX_U32 nRangeMappingChroma;
} OMX_TI_VIDEO_RANGEMAPPING;

/* ============================================================================= */
/*!
 @brief OMX_TI_VIDEO_RESCALINGMATRIX : Structure to access rescaled
                                       width/height generated by decoders
                                       as metatadata on its output port.
 @param nScaledHeight   Scaled image width for post processing for decoder.
 @param nScaledWidth    Scaled image height for post processing for decoder.
*/
/* ============================================================================= */

typedef struct OMX_TI_VIDEO_RESCALINGMATRIX {
	OMX_U32 nScaledHeight;
	OMX_U32 nScaledWidth;
} OMX_TI_VIDEO_RESCALINGMATRIX;


/*==========================================================================*/
/*!
 @brief OMX_TI_PARAM_PAYLOADHEADERFLAG : To specify the payload headerflag
                                         for VP6/VP7 decoder.
 @param bPayloadHeaderFlag      Flag - TRUE indicates that frame length and
                                timestamp(for IVF format) will be part of
                                frame input buffer.
                                Flag - FALSE indecates that frame length and
                                timestamp(for IVF format) will not be part of
                                the input buffer.
*/
/*==========================================================================*/

typedef struct OMX_TI_PARAM_PAYLOADHEADERFLAG {
	OMX_U32 nSize;
	OMX_VERSIONTYPE nVersion;
	OMX_BOOL bPayloadHeaderFlag;
} OMX_TI_PARAM_PAYLOADHEADERFLAG;


/*==========================================================================*/
/*!
@brief OMX_TI_PARAM_IVFFLAG : Suport added to handle IVF header Decoding Mode
@param bIvfFlag               TRUE enables IVF decoding mode.
                              FALSE indicates bitstream format is non-IVF.
*/
/*==========================================================================*/

typedef struct OMX_TI_PARAM_IVFFLAG {
	OMX_U32 nSize;
	OMX_VERSIONTYPE nVersion;
	OMX_BOOL bIvfFlag;
} OMX_TI_PARAM_IVFFLAG;

// A pointer to this struct is passed to OMX_SetParameter() when the extension
// index "OMX.google.android.index.storeMetaDataInBuffers"
// is given.
//
// When meta data is stored in the video buffers passed between OMX clients
// and OMX components, interpretation of the buffer data is up to the
// buffer receiver, and the data may or may not be the actual video data, but
// some information helpful for the receiver to locate the actual data.
// The buffer receiver thus needs to know how to interpret what is stored
// in these buffers, with mechanisms pre-determined externally. How to
// interpret the meta data is outside of the scope of this method.
//
// Currently, this is specifically used to pass meta data from video source
// (camera component, for instance) to video encoder to avoid memcpying of
// input video frame data. To do this, bStoreMetaDta is set to OMX_TRUE.
// If bStoreMetaData is set to false, real YUV frame data will be stored
// in the buffers. In addition, if no OMX_SetParameter() call is made
// with the corresponding extension index, real YUV data is stored
// in the buffers.
typedef struct OMX_VIDEO_STOREMETADATAINBUFFERSPARAMS {
    OMX_U32 nSize;
    OMX_VERSIONTYPE nVersion;
    OMX_U32 nPortIndex;
    OMX_BOOL bStoreMetaData;
} OMX_VIDEO_STOREMETADATAINBUFFERSPARAMS;


/**
 * Interlaced Video Content format
 *
 * STRUCT MEMBERS:
 *  nSize      : Size of the structure in bytes
 *  nVersion   : OMX specification version information
 *  nPortIndex : Port that this structure applies to
 *  nFormat    : bitmapped value indentifying the interlaced formats supported by component
 *  nTimeStamp : temporal timestamp information for the second field
 */
typedef struct OMX_TI_INTERLACEFORMATTYPE {
	OMX_U32 nSize;
	OMX_VERSIONTYPE nVersion;
	OMX_U32 nPortIndex;
	OMX_U32 nFormat;
	OMX_TICKS nTimeStamp;
} OMX_TI_INTERLACEFORMATTYPE;

/**
 * Interlace format types
 */
typedef enum OMX_TI_INTERLACETYPE {
	OMX_InterlaceFrameProgressive= 0x00,
	OMX_InterlaceInterleaveFrameTopFieldFirst= 0x01,
	OMX_InterlaceInterleaveFrameBottomFieldFirst= 0x02,
	OMX_InterlaceFrameTopFieldFirst= 0x04,
	OMX_InterlaceFrameBottomFieldFirst= 0x08,
	OMX_InterlaceInterleaveFieldTop= 0x10,
	OMX_InterlaceInterleaveFieldBottom= 0x20,
	OMX_InterlaceFmtMask= 0x7FFFFFFF
} OMX_TI_INTERLACETYPE;

/**
 * To query if the stream contains interlaced or progressive conten
 *
 * STRUCT MEMBERS:
 *  nSize             : Size of the structure in bytes
 *  nVersion          : OMX specification version information
 *  nPortIndex        : Port that this structure applies to
 *  bInterlaceFormat  : whether the stream contains interlace or progressive content
 *                        OMX_TRUE indicates interlace and OMX_FALSE indicates progressive
 *  nInterlaceFormats : bitmapped value identifying the interlace formats detected within the stream
 */
typedef struct OMX_TI_STREAMINTERLACEFORMATTYPE {
	OMX_U32 nSize;
	OMX_VERSIONTYPE nVersion;
	OMX_U32 nPortIndex;
	OMX_BOOL bInterlaceFormat;
	OMX_U32 nInterlaceFormats;
} OMX_TI_STREAMINTERLACEFORMAT;

/*!====================================================================!

    Currently we only support SVC baseline profile

 * !====================================================================!*/
    typedef enum OMX_TI_VIDEO_SVCPROFILETYPE {
        OMX_VIDEO_SVCProfileBaseline            = 0x01,     /**< Baseline profile */
        OMX_VIDEO_SVCProfileHigh                = 0x02,     /**< High profile */
        OMX_VIDEO_SVCProfileHighIntra           = 0x03,     /**< High Intra profile */
        OMX_VIDEO_SVCProfileMax                 = 0x7FFFFFFF
    } OMX_TI_VIDEO_SVCPROFILETYPE;


/*!====================================================================!

    Currently we support only SVC baseline profile upto level 4 for SVC encoder.

 * !====================================================================!*/
    typedef enum OMX_TI_VIDEO_SVCLEVELTYPE {
        OMX_VIDEO_SVCLevel1                     = 0x01,     /**< Level 1 */
        OMX_VIDEO_SVCLevel1b                    = 0x02,     /**< Level 1b */
        OMX_VIDEO_SVCLevel11                    = 0x04,     /**< Level 1.1 */
        OMX_VIDEO_SVCLevel12                    = 0x08,     /**< Level 1.2 */
        OMX_VIDEO_SVCLevel13                    = 0x10,     /**< Level 1.3 */
        OMX_VIDEO_SVCLevel2                     = 0x20,     /**< Level 2 */
        OMX_VIDEO_SVCLevel21                    = 0x40,     /**< Level 2.1 */
        OMX_VIDEO_SVCLevel22                    = 0x80,     /**< Level 2.2 */
        OMX_VIDEO_SVCLevel3                     = 0x100,    /**< Level 3 */
        OMX_VIDEO_SVCLevel31                    = 0x200,    /**< Level 3.1 */
        OMX_VIDEO_SVCLevel32                    = 0x400,    /**< Level 3.2 */
        OMX_VIDEO_SVCLevel4                     = 0x800,    /**< Level 4 */
        OMX_VIDEO_SVCLevel41                    = 0x1000,   /**< Level 4.1 */
        OMX_VIDEO_SVCLevel42                    = 0x2000,   /**< Level 4.2 */
        OMX_VIDEO_SVCLevel5                     = 0x4000,   /**< Level 5 */
        OMX_VIDEO_SVCLevel51                    = 0x8000,   /**< Level 5.1 */
        OMX_VIDEO_SVCLevelMax                   = 0x7FFFFFFF
    } OMX_TI_VIDEO_SVCLEVELTYPE;


    typedef struct OMX_VIDEO_SVC_STD_PARAMS {
        OMX_U32  nSliceHeaderSpacing;
        OMX_U32  nPFrames;
        OMX_U32  nBFrames;
        OMX_BOOL bUseHadamard;
        OMX_U32  nRefFrames;
        OMX_U32  nRefIdx10ActiveMinus1;
        OMX_U32  nRefIdx11ActiveMinus1;
        OMX_BOOL bEnableUEP;
        /* Not needed as per SVC encoder requirements
        OMX_BOOL                                bEnableFMO;
        OMX_BOOL                                bEnableASO;
        OMX_BOOL                                bEnableRS;
        */
        OMX_VIDEO_AVCLOOPFILTERTYPE eLoopFilterMode;
        OMX_U32                     nAllowedPictureTypes;
        OMX_BOOL                    bFrameMBsOnly;
        OMX_BOOL                    bMBAFF;
        OMX_BOOL                    bEntropyCodingCABAC;
        OMX_BOOL                    bWeightedPPrediction;
        OMX_U32                     nWeightedBipredicitonMode;
        OMX_BOOL                    bconstIpred;
        OMX_BOOL                    bDirect8x8Inference;
        OMX_BOOL                    bDirectSpatialTemporal;
        OMX_U32                     nCabacInitIdc;
    } OMX_VIDEO_SVC_STD_PARAMS;


    typedef struct OMX_VIDEO_SVC_RECTTYPE {
        OMX_S32 nLeft;
        OMX_S32 nTop;
        OMX_U32 nWidth;
        OMX_U32 nHeight;
    } OMX_VIDEO_SVC_RECTTYPE;


    typedef struct OMX_VIDEO_SVC_BITRATETYPE {
        OMX_VIDEO_CONTROLRATETYPE eControlRate;
        OMX_U32                   nTargetBitrate;
    } OMX_VIDEO_SVC_BITRATETYPE;


    typedef struct OMX_VIDEO_SVC_MOTIONVECTORTYPE {
        OMX_VIDEO_MOTIONVECTORTYPE eAccuracy;
        OMX_BOOL                   bUnrestrictedMVs;
        OMX_BOOL                   bFourMV;
        OMX_S32                    sXSearchRange;
        OMX_S32                    sYSearchRange;
    } OMX_VIDEO_SVC_MOTIONVECTORTYPE;


    typedef struct OMX_VIDEO_SVC_QUANTIZATIONTYPE {
        OMX_U32 nQpI;
        OMX_U32 nQpP;
        OMX_U32 nQpB;
    } OMX_VIDEO_SVC_QUANTIZATIONTYPE;


    typedef struct OMX_VIDEO_SVC_INTRAREFRESHTYPE {
        OMX_VIDEO_INTRAREFRESHTYPE eRefreshMode;
        OMX_U32                    nAirMBs;
        OMX_U32                    nAirRef;
        OMX_U32                    nCirMBs;
    } OMX_VIDEO_SVC_INTRAREFRESHTYPE;


    typedef struct OMX_VIDEO_SVC_VBSMCTYPE {
        OMX_BOOL b16x16;
        OMX_BOOL b16x8;
        OMX_BOOL b8x16;
        OMX_BOOL b8x8;
        OMX_BOOL b8x4;
        OMX_BOOL b4x8;
        OMX_BOOL b4x4;
    } OMX_VIDEO_SVC_VBSMCTYPE;


    typedef struct OMX_VIDEO_SVC_NALUCONTROLTYPE {
        OMX_U32 nStartofSequence;
        OMX_U32 nEndofSequence;
        OMX_U32 nIDR;
        OMX_U32 nIntraPicture;
        OMX_U32 nNonIntraPicture;
    }OMX_VIDEO_SVC_NALUCONTROLTYPE;


    typedef struct OMX_VIDEO_SVC_MEBLOCKSIZETYPE {
        OMX_VIDEO_BLOCKSIZETYPE eMinBlockSizeP;
        OMX_VIDEO_BLOCKSIZETYPE eMinBlockSizeB;
    }OMX_VIDEO_SVC_MEBLOCKSIZETYPE;


    typedef struct OMX_VIDEO_SVC_INTRAPREDTYPE {
        OMX_U32                       nLumaIntra4x4Enable;
        OMX_U32                       nLumaIntra8x8Enable;
        OMX_U32                       nLumaIntra16x16Enable;
        OMX_U32                       nChromaIntra8x8Enable;
        OMX_VIDEO_CHROMACOMPONENTTYPE eChromaComponentEnable;
    }OMX_VIDEO_SVC_INTRAPREDTYPE;


    typedef struct OMX_VIDEO_SVC_ENCODER_PRESETTYPE {
        OMX_VIDEO_ENCODING_MODE_PRESETTYPE eEncodingModePreset;
        OMX_VIDEO_RATECONTROL_PRESETTYPE   eRateControlPreset;
    }OMX_VIDEO_SVC_ENCODER_PRESETTYPE;


    typedef struct OMX_VIDEO_SVC_VUIINFOTYPE {
        OMX_BOOL                  bAspectRatioPresent;
        OMX_VIDEO_ASPECTRATIOTYPE ePixelAspectRatio;
        OMX_BOOL                  bFullRange;
    }OMX_VIDEO_SVC_VUIINFOTYPE;


    typedef struct OMX_VIDEO_SVC_HRDBUFFERSETTING {
        OMX_U32 nInitialBufferLevel;
        OMX_U32 nHRDBufferSize;
        OMX_U32 nTargetBitrate;
    }OMX_VIDEO_SVC_HRDBUFFERSETTING;


    typedef struct OMX_VIDEO_SVC_INTRAPERIOD {
        OMX_U32 nIDRPeriod;
        OMX_U32 nPFrames;
    } OMX_VIDEO_SVC_INTRAPERIOD;


    typedef struct OMX_VIDEO_SVC_PIXELINFOTYPE {
        OMX_U32 nWidth;
        OMX_U32 nHeight;
    } OMX_VIDEO_SVC_PIXELINFOTYPE;


    typedef struct OMX_VIDEO_SVC_MESEARCHRANGETYPE {
        OMX_VIDEO_MOTIONVECTORTYPE eMVAccuracy;
        OMX_U32                    nHorSearchRangeP;
        OMX_U32                    nVerSearchRangeP;
        OMX_U32                    nHorSearchRangeB;
        OMX_U32                    nVerSearchRangeB;
    }OMX_VIDEO_SVC_MESEARCHRANGETYPE;


    typedef struct OMX_VIDEO_SVC_QPSETTINGSTYPE {
        OMX_U32 nQpI;
        OMX_U32 nQpMaxI;
        OMX_U32 nQpMinI;
        OMX_U32 nQpP;
        OMX_U32 nQpMaxP;
        OMX_U32 nQpMinP;
        OMX_U32 nQpOffsetB;
        OMX_U32 nQpMaxB;
        OMX_U32 nQpMinB;
    }OMX_VIDEO_SVC_QPSETTINGSTYPE;


    typedef struct OMX_VIDEO_SVC_SLICECODINGTYPE {
        OMX_VIDEO_AVCSLICEMODETYPE eSliceMode;
        OMX_U32                    nSlicesize;
    }OMX_VIDEO_SVC_SLICECODINGTYPE;


    typedef struct OMX_VIDEO_EXEC_SVC_HRDBUFFERSETTING {
        OMX_U32 nHRDBufferSize;
        OMX_U32 nEncodeBitrate;
    }OMX_VIDEO_EXEC_SVC_HRDBUFFERSETTING;

/**
 * SVC params
 *
 * STRUCT MEMBERS:
 *  nSize                     : Size of the structure in bytes
 *  nVersion                  : OMX specification version information
 *  nPortIndex                : Port that this structure applies to
 *  nSliceHeaderSpacing       : Number of macroblocks between slice header, put
 *                              zero if not used
 *  nPFrames                  : Number of P frames between each I frame
 *  nBFrames                  : Number of B frames between each I frame
 *  bUseHadamard              : Enable/disable Hadamard transform
 *  nRefFrames                : Max number of reference frames to use for inter
 *                              motion search (1-16)
 *  nRefIdxTrailing           : Pic param set ref frame index (index into ref
 *                              frame buffer of trailing frames list), B frame
 *                              support
 *  nRefIdxForward            : Pic param set ref frame index (index into ref
 *                              frame buffer of forward frames list), B frame
 *                              support
 *  bEnableUEP                : Enable/disable unequal error protection. This
 *                              is only valid of data partitioning is enabled.
 *  bEnableFMO                : Enable/disable flexible macroblock ordering
 *  bEnableASO                : Enable/disable arbitrary slice ordering
 *  bEnableRS                 : Enable/disable sending of redundant slices
 *  eProfile                  : AVC profile(s) to use
 *  eLevel                    : AVC level(s) to use
 *  nAllowedPictureTypes      : Specifies the picture types allowed in the
 *                              bitstream
 *  bFrameMBsOnly             : specifies that every coded picture of the
 *                              coded video sequence is a coded frame
 *                              containing only frame macroblocks
 *  bMBAFF                    : Enable/disable switching between frame and
 *                              field macroblocks within a picture
 *  bEntropyCodingCABAC       : Entropy decoding method to be applied for the
 *                              syntax elements for which two descriptors appear
 *                              in the syntax tables
 *  bWeightedPPrediction      : Enable/disable weighted prediction shall not
 *                              be applied to P and SP slices
 *  nWeightedBipredicitonMode : Default weighted prediction is applied to B
 *                              slices
 *  bconstIpred               : Enable/disable intra prediction
 *  bDirect8x8Inference       : Specifies the method used in the derivation
 *                              process for luma motion vectors for B_Skip,
 *                              B_Direct_16x16 and B_Direct_8x8 as specified
 *                              in subclause 8.4.1.2 of the AVC spec
 *  bDirectSpatialTemporal    : Flag indicating spatial or temporal direct
 *                              mode used in B slice coding (related to
 *                              bDirect8x8Inference) . Spatial direct mode is
 *                              more common and should be the default.
 *  nCabacInitIdx             : Index used to init CABAC contexts
 *  eLoopFilterMode           : Enable/disable loop filter
 */
    typedef struct OMX_TI_VIDEO_PARAM_SVCTYPE {
        OMX_U32         nSize;
        OMX_VERSIONTYPE nVersion;
        OMX_U32         nPortIndex;

        OMX_U32                nActualFrameWidth;
        OMX_U32                nActualFrameHeight;
        OMX_S32                nStride;
        OMX_U32                xFramerate;
        OMX_COLOR_FORMATTYPE   eColorFormat;
        OMX_VIDEO_SVC_RECTTYPE sRecType;

        OMX_VIDEO_SVC_STD_PARAMS sBasicParams;

        OMX_U32                     nRefFrames;
        OMX_TI_VIDEO_SVCPROFILETYPE eProfile;
        OMX_TI_VIDEO_SVCLEVELTYPE   eLevel;

        OMX_U32                   xEncodeFramerate;
        OMX_VIDEO_SVC_BITRATETYPE sBitRateParams;

        OMX_VIDEO_SVC_MOTIONVECTORTYPE sMotionVectorParams;
        OMX_VIDEO_SVC_QUANTIZATIONTYPE sQuantizationParams;
        OMX_VIDEO_SVC_INTRAREFRESHTYPE sIntraRefreshParams;
        OMX_VIDEO_SVC_VBSMCTYPE        sVBSMCParams;

        //OMX_NALUFORMATSTYPE               eNaluFormat;
        OMX_VIDEO_SVC_NALUCONTROLTYPE sNalUnitParams;

        OMX_VIDEO_SVC_MEBLOCKSIZETYPE    sMEBlockSizeParams;
        OMX_VIDEO_SVC_INTRAPREDTYPE      sIntraPredParams;
        OMX_VIDEO_SVC_ENCODER_PRESETTYPE sEncPresetParams;
        OMX_VIDEO_TRANSFORMBLOCKSIZETYPE eTransformBlocksize;
        OMX_VIDEO_SVC_VUIINFOTYPE        sVUIInfoParams;
        OMX_VIDEO_SVC_HRDBUFFERSETTING   sHRDBufferParams;

        OMX_U32 nNumTemporalLayers;
        OMX_S32 nDependencyID;
        OMX_S32 nQualityID;
        //OMX_VIDEO_SVC_ENCODE_MODE         eModeOfEncode;

        OMX_U32 nErrorConcealmentMode;
        OMX_U32 nDeblockFilterMode;
    } OMX_TI_VIDEO_PARAM_SVCTYPE;

 typedef struct OMX_TI_VIDEO_CONFIG_SVCLAYERDETAILS {
    OMX_U32         nSize;
    OMX_VERSIONTYPE nVersion;
    OMX_U32         nPortIndex;

    OMX_U32 nNumLayers;
    OMX_U32 nLayerId;
    OMX_U8  nPriorityId;
    OMX_U8  nDependencyId;
    OMX_U8  nQualityId;
    OMX_U8  nTemporalId;
    OMX_U8  nBitrateInfoPresentFlag;
    OMX_U8  nFramerateInfoPresentFlag;
    OMX_U8  nFramesizeInfoPresentFlag;
    OMX_U16 nAvgBitrate;
    OMX_U16 nMaxBitrate;
    OMX_U16 nAvgFramerate;
    OMX_U32 nFrameWidth;
    OMX_U32 nFrameHeight;

    OMX_U32 nLayerIndex;     /* Used to query for individual layer details */

} OMX_TI_VIDEO_CONFIG_SVCLAYERDETAILS;

typedef struct OMX_TI_VIDEO_CONFIG_SVCTARGETLAYER {
    OMX_U32         nSize;
    OMX_VERSIONTYPE nVersion;
    OMX_U32         nPortIndex;

    OMX_U32 nSvcTargetLayerDID;
    OMX_U32 nSvcTargetLayerTID;
    OMX_U32 nSvcTargetLayerQID;

} OMX_TI_VIDEO_CONFIG_SVCTARGETLAYER;
/* ========================================================================== */
/*!
@brief OMX_TI_VIDEO_SLICEDATAINFO : to configure the Slice Settings
@param  nNumofSlices   number of validfields to be read
@param  nSliceSizeConfigured   variable that indicates the MaxSlice configured
                               & (n*nSliceSizeConfigured) gives the buff offset
                               for nth slice in the o/p buffer
@param  nSliceSize   gives the SliceSize
*/
/* ==========================================================================*/
typedef struct OMX_TI_VIDEO_SLICEDATAINFO {
    OMX_U32 nNumofSlices;
    OMX_U32 nSliceSizeConfigured;
    OMX_U32 nSliceSize[OMXH264E_MAX_SLICE_SUPPORTED];
} OMX_TI_VIDEO_SLICEDATAINFO;
/**
*	@brief	mode selection for the data that is given to the Codec
 */

typedef enum _OMX_VIDEO_AVCLTRMODE {
  OMX_H264ENC_LTRP_NONE = 0,
        /**< No longterm refernce frame in the sequnce
           */
  OMX_H264ENC_LTRP_REFERTOIDR = 1,
          /**< Mark all the I frames as long term-reference frames and
           * based on the frame control IH264ENC_Control, refere to
           * a long-term reference frame (I frame).
           */
  OMX_H264ENC_LTRP_REFERTOP_PROACTIVE =2,
          /**< Two long term frames are supported in this schme and
           * long-term index marking and refernce frame update is done based
           * the IH264ENC_Control values
           */
  OMX_H264ENC_LTRP_REFERTOP_REACTIVE = 3
          /**< This is not supported in the current version of encoder
           */
} OMX_VIDEO_AVCLTRMODE;


/* ============================================================================= */
/*
@brief OMX_TI_VIDEO_PARAM_AVC_LTRP : Structure to enable the configuration of Long Term reference Picture feature in H264 Encoder for the session
Enabling this parameter will instruct encoder to keep its recent I/IDR frame in its reference buffer list.
So it increases the DDR foot print by one frame buffer
@param eLTRMode	: enables the LongTerm Reference Picture, possible modes: 0, 1, 2
@param nLTRInterval : interval of the write indicating to codec interms of the frame number
*/
/* ============================================================================= */
typedef struct OMX_TI_VIDEO_PARAM_AVC_LTRP{
	OMX_U32 nSize;
	OMX_VERSIONTYPE nVersion;
	OMX_U32 nPortIndex;
	OMX_VIDEO_AVCLTRMODE eLTRMode;
	OMX_U32 nLTRInterval;
} OMX_TI_VIDEO_PARAM_AVC_LTRP;

/*
@brief OMX_TI_VIDEO_CONFIG_AVC_LTRP : Structure to provide the configuration to acknowledge successful decode of previous LTR
@param eLTRFrameDecoded	: tells the decoder that the LTR has been decoded successfully when set to TRUE
*/
/* ============================================================================= */
typedef struct OMX_TI_VIDEO_CONFIG_AVC_LTRP{
    OMX_U32 nSize;
    OMX_VERSIONTYPE nVersion;
    OMX_U32 nPortIndex;
    OMX_BOOL bEnableNextLTR;
} OMX_TI_VIDEO_CONFIG_AVC_LTRP;

/* ============================================================================= */
/*
@brief OMX_TI_VIDEO_CONFIG_AVC_LTRP_INTERVAL : Structure to enable the update of the LTRP
Interval during runtime
@param nLTRInterval : interval of the write indicating to codec interms of the frame number
*/
/* ============================================================================= */
typedef struct OMX_TI_VIDEO_CONFIG_AVC_LTRP_INTERVAL{
	OMX_U32 nSize;
	OMX_VERSIONTYPE nVersion;
	OMX_U32 nPortIndex;
	OMX_U32 nLTRInterval;
} OMX_TI_VIDEO_CONFIG_AVC_LTRP_INTERVAL;

/* ============================================================================= */
/*
@brief OMX_TI_VIDEO_CONFIG_AVC_LTRP_INTERVAL : Structure to enable timestamps in decode order
            at i/p of decoders.
*/
/* ============================================================================= */
typedef struct OMX_TI_PARAM_TIMESTAMP_IN_DECODE_ORDER{
	OMX_U32 nSize;
	OMX_VERSIONTYPE nVersion;
        OMX_BOOL bEnabled;
} OMX_TI_PARAM_TIMESTAMP_IN_DECODE_ORDER;

#endif /* OMX_TI_VIDEO_H */

